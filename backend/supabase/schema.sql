-- Valões AquaGestor: banco do servidor (Supabase / Postgres).
-- Rode este arquivo inteiro no SQL Editor do projeto (uma vez). Depende de specs/02-dominio.md e specs/05-offline-sync.md.
--
-- Como a sincronização funciona aqui:
--   * Cada registro tem `uuid` (identidade), `atualizado_em` (relógio do aparelho, em ms) e `excluido`.
--   * O aparelho envia com "upsert por uuid": reenviar o mesmo registro nunca cria cópia (caso G1).
--   * Em conflito vence o maior `atualizado_em` (caso G2): o gatilho `sync_antes_de_gravar` ignora versão mais velha.
--   * `servidor_em` é o relógio do servidor; os aparelhos baixam "o que mudou depois de X" por ele.
--   * Exclusão é lógica (`excluido = true`): a linha fica como túmulo para os outros aparelhos saberem (caso G3).
--   * Toda linha pertence a uma fazenda (`fazenda_id`); as regras de acesso (RLS) só deixam cada usuário
--     ver e gravar a própria fazenda. Só o ADMIN mexe em usuários.

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------- fazenda e usuários

create table public.fazenda (
    id uuid primary key default gen_random_uuid(),
    criado_em timestamptz not null default now()
);

create table public.usuario (
    uuid uuid primary key,
    fazenda_id uuid not null references public.fazenda (id),
    -- Preenchido quando a pessoa cria a conta; fica vazio enquanto o convite está pendente.
    auth_user_id uuid unique references auth.users (id) on delete set null,
    nome text not null,
    email text not null,
    papel text not null check (papel in ('ADMIN', 'FUNCIONARIO')),
    status text not null check (status in ('ATIVO', 'CONVIDADO', 'DESATIVADO')),
    atualizado_em bigint not null,
    excluido boolean not null default false,
    servidor_em timestamptz not null default clock_timestamp()
);
create unique index usuario_email_por_fazenda on public.usuario (fazenda_id, lower(email));

-- Fazenda de quem está logado (só conta usuário ATIVO). Security definer: a regra de acesso não pode
-- depender da própria tabela que ela protege.
create function public.minha_fazenda() returns uuid
language sql stable security definer set search_path = public as $$
    select fazenda_id from public.usuario
    where auth_user_id = auth.uid() and status = 'ATIVO' and not excluido
    limit 1
$$;

create function public.sou_admin() returns boolean
language sql stable security definer set search_path = public as $$
    select exists (
        select 1 from public.usuario
        where auth_user_id = auth.uid() and status = 'ATIVO' and papel = 'ADMIN' and not excluido
    )
$$;

alter table public.usuario alter column fazenda_id set default public.minha_fazenda();

-- ---------------------------------------------------------------- dados da fazenda
-- Colunas em snake_case; referências por uuid (o id local do aparelho não existe aqui).
-- Unidade no nome da coluna, como no app: _kg, _g, _mg_l, _cm, _c, _m2, _m3.

create table public.tanque (
    uuid uuid primary key,
    fazenda_id uuid not null default public.minha_fazenda() references public.fazenda (id),
    nome text not null,
    tipo text not null,
    area_m2 double precision,
    profundidade_media_m double precision,
    volume_m3 double precision,
    usa_aerador boolean not null,
    aerador_potencia_hp double precision,
    renovacao_agua_pct_dia double precision not null,
    status text not null,
    atualizado_em bigint not null,
    excluido boolean not null default false,
    servidor_em timestamptz not null default clock_timestamp()
);

create table public.lote (
    uuid uuid primary key,
    fazenda_id uuid not null default public.minha_fazenda() references public.fazenda (id),
    tanque_uuid uuid not null references public.tanque (uuid),
    especie text not null,
    fase text not null,
    quantidade_inicial integer not null,
    peso_medio_inicial_g double precision not null,
    data_povoamento bigint not null,
    peso_medio_atual_g double precision not null,
    origem_alevinos text,
    status text not null,
    data_encerramento bigint,
    atualizado_em bigint not null,
    excluido boolean not null default false,
    servidor_em timestamptz not null default clock_timestamp()
);

create table public.qualidade_agua (
    uuid uuid primary key,
    fazenda_id uuid not null default public.minha_fazenda() references public.fazenda (id),
    tanque_uuid uuid not null references public.tanque (uuid),
    data_hora bigint not null,
    momento text not null,
    transparencia_cm double precision,
    temperatura_c double precision,
    oxigenio_mg_l double precision,
    ph double precision,
    amonia_total_mg_l double precision,
    nitrito_mg_l double precision,
    alcalinidade_mg_l double precision,
    dureza_mg_l double precision,
    dia_nublado boolean not null default false,
    criado_por_user_id text,
    atualizado_em bigint not null,
    excluido boolean not null default false,
    servidor_em timestamptz not null default clock_timestamp()
);

create table public.arracoamento (
    uuid uuid primary key,
    fazenda_id uuid not null default public.minha_fazenda() references public.fazenda (id),
    lote_uuid uuid not null references public.lote (uuid),
    data_hora bigint not null,
    quantidade_kg double precision not null,
    suspenso boolean not null default false,
    motivo_suspensao text,
    marca_racao text,
    proteina_bruta_pct double precision,
    tamanho_pellet_mm double precision,
    comportamento text not null,
    sobra_racao boolean not null default false,
    criado_por_user_id text,
    atualizado_em bigint not null,
    excluido boolean not null default false,
    servidor_em timestamptz not null default clock_timestamp()
);

create table public.biometria (
    uuid uuid primary key,
    fazenda_id uuid not null default public.minha_fazenda() references public.fazenda (id),
    lote_uuid uuid not null references public.lote (uuid),
    data_hora bigint not null,
    peso_amostra_kg double precision not null,
    numero_peixes_amostra integer not null,
    observacao_saude text,
    criado_por_user_id text,
    atualizado_em bigint not null,
    excluido boolean not null default false,
    servidor_em timestamptz not null default clock_timestamp()
);

create table public.movimentacao_lote (
    uuid uuid primary key,
    fazenda_id uuid not null default public.minha_fazenda() references public.fazenda (id),
    lote_uuid uuid not null references public.lote (uuid),
    data_hora bigint not null,
    tipo text not null,
    quantidade integer not null,
    peso_total_kg double precision,
    tanque_destino_uuid uuid references public.tanque (uuid),
    observacao text,
    criado_por_user_id text,
    atualizado_em bigint not null,
    excluido boolean not null default false,
    servidor_em timestamptz not null default clock_timestamp()
);

-- ---------------------------------------------------------------- gatilho de sincronização

-- Versão mais velha (ou igual) que a guardada é descartada em silêncio: vence o maior atualizado_em, e
-- reenviar o mesmo registro não muda nada (nem o servidor_em, então ninguém baixa de novo à toa).
create function public.sync_antes_de_gravar() returns trigger language plpgsql as $$
begin
    if tg_op = 'UPDATE' and new.atualizado_em <= old.atualizado_em then
        return null;
    end if;
    new.servidor_em := clock_timestamp();
    return new;
end
$$;

-- ---------------------------------------------------------------- novo usuário (cadastro)

-- Quem foi convidado entra na fazenda do convite, com o papel combinado; quem não foi cria a própria
-- fazenda e vira ADMIN (decisão de specs/11-autenticacao.md).
create function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
declare
    convite public.usuario%rowtype;
    nova_fazenda uuid;
    nome_informado text := nullif(trim(coalesce(new.raw_user_meta_data ->> 'nome', '')), '');
    agora_ms bigint := (extract(epoch from clock_timestamp()) * 1000)::bigint;
begin
    select * into convite from public.usuario
    where lower(email) = lower(new.email) and status = 'CONVIDADO' and auth_user_id is null and not excluido
    limit 1;

    if found then
        update public.usuario
        set auth_user_id = new.id,
            status = 'ATIVO',
            nome = coalesce(nome_informado, nome),
            atualizado_em = greatest(atualizado_em + 1, agora_ms)
        where uuid = convite.uuid;
    else
        insert into public.fazenda default values returning id into nova_fazenda;
        insert into public.usuario (uuid, fazenda_id, auth_user_id, nome, email, papel, status, atualizado_em)
        values (
            gen_random_uuid(), nova_fazenda, new.id,
            coalesce(nome_informado, split_part(new.email, '@', 1)),
            lower(new.email), 'ADMIN', 'ATIVO', agora_ms
        );
    end if;
    return new;
end
$$;

create trigger on_auth_user_created after insert on auth.users
    for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------- índices, gatilhos e regras de acesso

alter table public.fazenda enable row level security; -- sem política: o app não lê nem grava fazendas

do $$
declare
    t text;
begin
    foreach t in array array['tanque', 'lote', 'qualidade_agua', 'arracoamento', 'biometria', 'movimentacao_lote', 'usuario']
    loop
        execute format('create index %I on public.%I (servidor_em)', t || '_servidor_em', t);
        execute format(
            'create trigger sync_antes_de_gravar before insert or update on public.%I for each row execute function public.sync_antes_de_gravar()',
            t
        );
        execute format('alter table public.%I enable row level security', t);
        execute format(
            'create policy "ler a propria fazenda" on public.%I for select to authenticated using (fazenda_id = public.minha_fazenda())',
            t
        );
    end loop;

    -- Registrar dados: qualquer usuário ativo da fazenda (ADMIN ou FUNCIONARIO), `specs/06-perfis-permissoes.md`.
    foreach t in array array['tanque', 'lote', 'qualidade_agua', 'arracoamento', 'biometria', 'movimentacao_lote']
    loop
        execute format(
            'create policy "gravar na propria fazenda" on public.%I for insert to authenticated with check (fazenda_id = public.minha_fazenda())',
            t
        );
        execute format(
            'create policy "alterar na propria fazenda" on public.%I for update to authenticated using (fazenda_id = public.minha_fazenda()) with check (fazenda_id = public.minha_fazenda())',
            t
        );
    end loop;
end
$$;

-- Usuários: só o ADMIN convida, troca papel e desativa.
create policy "admin convida" on public.usuario for insert to authenticated
    with check (fazenda_id = public.minha_fazenda() and public.sou_admin());
create policy "admin altera" on public.usuario for update to authenticated
    using (fazenda_id = public.minha_fazenda() and public.sou_admin())
    with check (fazenda_id = public.minha_fazenda() and public.sou_admin());

-- ---------------------------------------------------------------- permissões da API

revoke all on all tables in schema public from anon;
grant usage on schema public to authenticated;
grant select, insert, update on
    public.usuario, public.tanque, public.lote, public.qualidade_agua,
    public.arracoamento, public.biometria, public.movimentacao_lote
    to authenticated;
-- Nenhuma tabela concede delete: a exclusão é sempre lógica.
