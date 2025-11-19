--
-- PostgreSQL database dump
--

-- Dumped from database version 15.15 (Debian 15.15-1.pgdg13+1)
-- Dumped by pg_dump version 16.9 (Ubuntu 16.9-0ubuntu0.24.10.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: unaccent; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;


--
-- Name: EXTENSION unaccent; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION unaccent IS 'text search dictionary that removes accents';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: customer; Type: TABLE; Schema: public; Owner: casero
--

CREATE TABLE public.customer (
    id bigint NOT NULL,
    name text NOT NULL,
    sector_id bigint NOT NULL,
    address text NOT NULL,
    debt integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.customer OWNER TO casero;

--
-- Name: customer_id_seq; Type: SEQUENCE; Schema: public; Owner: casero
--

CREATE SEQUENCE public.customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.customer_id_seq OWNER TO casero;

--
-- Name: customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: casero
--

ALTER SEQUENCE public.customer_id_seq OWNED BY public.customer.id;


--
-- Name: sector; Type: TABLE; Schema: public; Owner: casero
--

CREATE TABLE public.sector (
    id bigint NOT NULL,
    name text NOT NULL
);


ALTER TABLE public.sector OWNER TO casero;

--
-- Name: sector_id_seq; Type: SEQUENCE; Schema: public; Owner: casero
--

CREATE SEQUENCE public.sector_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sector_id_seq OWNER TO casero;

--
-- Name: sector_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: casero
--

ALTER SEQUENCE public.sector_id_seq OWNED BY public.sector.id;


--
-- Name: statistic; Type: TABLE; Schema: public; Owner: casero
--

CREATE TABLE public.statistic (
    id bigint NOT NULL,
    type character varying(32) NOT NULL,
    amount integer NOT NULL,
    sale_type character varying(32),
    items_count integer,
    date date NOT NULL
);


ALTER TABLE public.statistic OWNER TO casero;

--
-- Name: statistic_id_seq; Type: SEQUENCE; Schema: public; Owner: casero
--

CREATE SEQUENCE public.statistic_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.statistic_id_seq OWNER TO casero;

--
-- Name: statistic_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: casero
--

ALTER SEQUENCE public.statistic_id_seq OWNED BY public.statistic.id;


--
-- Name: transaction; Type: TABLE; Schema: public; Owner: casero
--

CREATE TABLE public.transaction (
    id bigint NOT NULL,
    customer_id bigint NOT NULL,
    date date NOT NULL,
    detail text NOT NULL,
    amount integer NOT NULL,
    balance integer NOT NULL,
    type character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    item_count integer
);


ALTER TABLE public.transaction OWNER TO casero;

--
-- Name: transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: casero
--

CREATE SEQUENCE public.transaction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transaction_id_seq OWNER TO casero;

--
-- Name: transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: casero
--

ALTER SEQUENCE public.transaction_id_seq OWNED BY public.transaction.id;


--
-- Name: customer id; Type: DEFAULT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.customer ALTER COLUMN id SET DEFAULT nextval('public.customer_id_seq'::regclass);


--
-- Name: sector id; Type: DEFAULT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.sector ALTER COLUMN id SET DEFAULT nextval('public.sector_id_seq'::regclass);


--
-- Name: statistic id; Type: DEFAULT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.statistic ALTER COLUMN id SET DEFAULT nextval('public.statistic_id_seq'::regclass);


--
-- Name: transaction id; Type: DEFAULT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.transaction ALTER COLUMN id SET DEFAULT nextval('public.transaction_id_seq'::regclass);


--
-- Name: customer customer_pkey; Type: CONSTRAINT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);


--
-- Name: sector sector_name_key; Type: CONSTRAINT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.sector
    ADD CONSTRAINT sector_name_key UNIQUE (name);


--
-- Name: sector sector_pkey; Type: CONSTRAINT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.sector
    ADD CONSTRAINT sector_pkey PRIMARY KEY (id);


--
-- Name: statistic statistic_pkey; Type: CONSTRAINT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.statistic
    ADD CONSTRAINT statistic_pkey PRIMARY KEY (id);


--
-- Name: transaction transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_pkey PRIMARY KEY (id);


--
-- Name: idx_statistic_date; Type: INDEX; Schema: public; Owner: casero
--

CREATE INDEX idx_statistic_date ON public.statistic USING btree (date);


--
-- Name: idx_statistic_type; Type: INDEX; Schema: public; Owner: casero
--

CREATE INDEX idx_statistic_type ON public.statistic USING btree (type);


--
-- Name: idx_transaction_customer_created_at; Type: INDEX; Schema: public; Owner: casero
--

CREATE INDEX idx_transaction_customer_created_at ON public.transaction USING btree (customer_id, created_at);


--
-- Name: idx_transaction_customer_date; Type: INDEX; Schema: public; Owner: casero
--

CREATE INDEX idx_transaction_customer_date ON public.transaction USING btree (customer_id, date);


--
-- Name: idx_transaction_type; Type: INDEX; Schema: public; Owner: casero
--

CREATE INDEX idx_transaction_type ON public.transaction USING btree (type);


--
-- Name: customer customer_sector_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_sector_id_fkey FOREIGN KEY (sector_id) REFERENCES public.sector(id);


--
-- Name: transaction transaction_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: casero
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customer(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--
