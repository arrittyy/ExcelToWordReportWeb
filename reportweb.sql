--
-- PostgreSQL database dump
--

\restrict hyD0zWx5tScPbDf4SP521k0MtNQSJKtDYoH2seUPD34e2f7nbCRG6tGA9TjnARa

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: approval_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.approval_log (
    id bigint NOT NULL,
    project_id integer NOT NULL,
    track character varying(20) NOT NULL,
    action character varying(20) NOT NULL,
    actor_name character varying(200),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.approval_log OWNER TO postgres;

--
-- Name: approval_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.approval_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.approval_log_id_seq OWNER TO postgres;

--
-- Name: approval_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.approval_log_id_seq OWNED BY public.approval_log.id;


--
-- Name: experiment_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.experiment_types (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(20) NOT NULL,
    table_schema text NOT NULL,
    report_fields_schema text NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


ALTER TABLE public.experiment_types OWNER TO postgres;

--
-- Name: experiment_types_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.experiment_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.experiment_types_id_seq OWNER TO postgres;

--
-- Name: experiment_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.experiment_types_id_seq OWNED BY public.experiment_types.id;


--
-- Name: image_attachments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.image_attachments (
    id integer NOT NULL,
    report_id integer NOT NULL,
    image_urls text NOT NULL,
    description text,
    display_order integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.image_attachments OWNER TO postgres;

--
-- Name: image_attachments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.image_attachments_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.image_attachments_id_seq OWNER TO postgres;

--
-- Name: image_attachments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.image_attachments_id_seq OWNED BY public.image_attachments.id;


--
-- Name: images; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.images (
    id integer NOT NULL,
    file_name character varying(255) NOT NULL,
    storage_path character varying(500) NOT NULL,
    file_size bigint NOT NULL,
    mime_type character varying(100),
    user_id character varying(450) NOT NULL,
    uploaded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    report_id integer
);


ALTER TABLE public.images OWNER TO postgres;

--
-- Name: COLUMN images.report_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.images.report_id IS '关联的报告ID,用于权限验证';


--
-- Name: images_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.images_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.images_id_seq OWNER TO postgres;

--
-- Name: images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.images_id_seq OWNED BY public.images.id;


--
-- Name: instruments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.instruments (
    id integer NOT NULL,
    instrument_name character varying(255) NOT NULL,
    instrument_model character varying(255),
    instrument_number character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.instruments OWNER TO postgres;

--
-- Name: TABLE instruments; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.instruments IS '全局仪器设备库（所有用户共享）';


--
-- Name: COLUMN instruments.instrument_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.instruments.instrument_name IS '仪器名称';


--
-- Name: COLUMN instruments.instrument_model; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.instruments.instrument_model IS '仪器型号';


--
-- Name: COLUMN instruments.instrument_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.instruments.instrument_number IS '仪器编号';


--
-- Name: COLUMN instruments.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.instruments.created_at IS '创建时间';


--
-- Name: COLUMN instruments.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.instruments.updated_at IS '更新时间';


--
-- Name: instruments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.instruments_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.instruments_id_seq OWNER TO postgres;

--
-- Name: instruments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.instruments_id_seq OWNED BY public.instruments.id;


--
-- Name: power_plants; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.power_plants (
    id integer NOT NULL,
    name character varying(200) NOT NULL,
    region character varying(100) NOT NULL,
    short_name character varying(100),
    province character varying(50) NOT NULL,
    city character varying(50) NOT NULL,
    address character varying(500) NOT NULL,
    phone character varying(50),
    fax character varying(50),
    remark character varying(1000),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.power_plants OWNER TO postgres;

--
-- Name: power_plants_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.power_plants_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.power_plants_id_seq OWNER TO postgres;

--
-- Name: power_plants_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.power_plants_id_seq OWNED BY public.power_plants.id;


--
-- Name: project_components; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.project_components (
    id integer NOT NULL,
    project_id integer NOT NULL,
    component_name character varying(255) NOT NULL,
    material character varying(100),
    category character varying(100),
    pipe_diameter character varying(50),
    wall_thickness character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    remark character varying(500)
);


ALTER TABLE public.project_components OWNER TO postgres;

--
-- Name: project_components_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.project_components_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.project_components_id_seq OWNER TO postgres;

--
-- Name: project_components_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.project_components_id_seq OWNED BY public.project_components.id;


--
-- Name: project_instruments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.project_instruments (
    id integer NOT NULL,
    project_id integer NOT NULL,
    instrument_name character varying(255) NOT NULL,
    instrument_model character varying(200),
    instrument_number character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    global_instrument_id integer,
    is_default boolean DEFAULT false,
    experiment_type_code character varying(20)
);


ALTER TABLE public.project_instruments OWNER TO postgres;

--
-- Name: TABLE project_instruments; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.project_instruments IS '项目仪器设备表';


--
-- Name: COLUMN project_instruments.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.project_instruments.id IS '主键ID';


--
-- Name: COLUMN project_instruments.project_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.project_instruments.project_id IS '项目ID';


--
-- Name: COLUMN project_instruments.instrument_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.project_instruments.instrument_name IS '仪器名称';


--
-- Name: COLUMN project_instruments.instrument_model; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.project_instruments.instrument_model IS '仪器型号';


--
-- Name: COLUMN project_instruments.instrument_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.project_instruments.instrument_number IS '仪器编号';


--
-- Name: project_instruments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.project_instruments_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.project_instruments_id_seq OWNER TO postgres;

--
-- Name: project_instruments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.project_instruments_id_seq OWNED BY public.project_instruments.id;


--
-- Name: projects; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.projects (
    id integer NOT NULL,
    project_number character varying(50) NOT NULL,
    third_party_project_number character varying(100),
    third_party_name character varying(200),
    project_name character varying(200) NOT NULL,
    project_type character varying(50),
    customer_name character varying(200) NOT NULL,
    user_id character varying(450) NOT NULL,
    start_date date NOT NULL,
    end_date date,
    status character varying(20) DEFAULT 'InProgress'::character varying NOT NULL,
    description character varying(1000),
    selected_experiment_type_ids text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    responsible_person character varying(100),
    reviewer_ndt character varying(100),
    review_date_ndt date,
    approver_ndt character varying(100),
    approval_date_ndt date,
    staff character varying(200),
    reviewer_chem character varying(100),
    review_date_chem date,
    approver_chem character varying(100),
    approval_date_chem date,
    writer_ndt character varying(100),
    writer_date_ndt date,
    ndt_signature_levels jsonb,
    third_party_approval_by_experiment_type jsonb,
    writer_chem character varying(100),
    writer_date_chem date,
    customer character varying(200),
    customer_contact character varying(100),
    power_plant_id integer,
    unit_id integer,
    approval_step_ndt integer DEFAULT 0 NOT NULL,
    approval_step_chem integer DEFAULT 0 NOT NULL,
    rejection_step_ndt integer,
    rejection_step_chem integer,
    aggregate_detection_log_order text
);


ALTER TABLE public.projects OWNER TO postgres;

--
-- Name: projects_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.projects_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.projects_id_seq OWNER TO postgres;

--
-- Name: projects_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.projects_id_seq OWNED BY public.projects.id;


--
-- Name: report_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.report_items (
    id integer NOT NULL,
    report_id integer NOT NULL,
    experiment_type_id integer NOT NULL,
    table_data text NOT NULL,
    summary text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.report_items OWNER TO postgres;

--
-- Name: report_items_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.report_items_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.report_items_id_seq OWNER TO postgres;

--
-- Name: report_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.report_items_id_seq OWNED BY public.report_items.id;


--
-- Name: reports; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reports (
    id integer NOT NULL,
    project_id integer NOT NULL,
    title character varying(200) NOT NULL,
    report_number character varying(50) NOT NULL,
    user_id character varying(450) NOT NULL,
    experiment_type_id integer NOT NULL,
    project_component_id integer,
    inspector character varying(300) DEFAULT '/'::character varying NOT NULL,
    test_method character varying(200),
    equipment character varying(200),
    test_standard character varying(200),
    component_name character varying(200),
    equipment_category character varying(100),
    equipment_name character varying(200),
    component_spec character varying(500),
    project_component_ids jsonb,
    instrument_model character varying(200),
    instrument_number character varying(100),
    test_date date NOT NULL,
    location character varying(200) DEFAULT '/'::character varying NOT NULL,
    report_image character varying(500),
    has_defect character varying(10),
    status character varying(20) DEFAULT 'Draft'::character varying NOT NULL,
    custom_fields jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    project_instrument_id integer,
    detection_content jsonb
);


ALTER TABLE public.reports OWNER TO postgres;

--
-- Name: COLUMN reports.project_instrument_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.reports.project_instrument_id IS '关联仪器设备ID';


--
-- Name: reports_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.reports_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.reports_id_seq OWNER TO postgres;

--
-- Name: reports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.reports_id_seq OWNED BY public.reports.id;


--
-- Name: unit_components; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.unit_components (
    id integer NOT NULL,
    unit_id integer NOT NULL,
    component_name character varying(255) NOT NULL,
    material character varying(100),
    category character varying(100),
    pipe_diameter character varying(50),
    wall_thickness character varying(50),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.unit_components OWNER TO postgres;

--
-- Name: unit_components_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.unit_components_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.unit_components_id_seq OWNER TO postgres;

--
-- Name: unit_components_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.unit_components_id_seq OWNED BY public.unit_components.id;


--
-- Name: units; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.units (
    id integer NOT NULL,
    power_plant_id integer NOT NULL,
    unit_name character varying(100) NOT NULL,
    unit_number character varying(50),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    installed_capacity character varying(50)
);


ALTER TABLE public.units OWNER TO postgres;

--
-- Name: units_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.units_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.units_id_seq OWNER TO postgres;

--
-- Name: units_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.units_id_seq OWNED BY public.units.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id character varying(450) NOT NULL,
    username character varying(256),
    normalized_username character varying(256),
    email character varying(256),
    normalized_email character varying(256),
    email_confirmed boolean,
    password_hash character varying(500),
    security_stamp character varying(500),
    concurrency_stamp character varying(500),
    phone_number character varying(50),
    phone_number_confirmed boolean,
    two_factor_enabled boolean,
    lockout_end timestamp without time zone,
    lockout_enabled boolean,
    access_failed_count integer,
    full_name character varying(200),
    department character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    role character varying(50) DEFAULT 'USER'::character varying,
    parent_user_id character varying(255),
    CONSTRAINT chk_user_role CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'USER'::character varying, 'SUB_USER'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: COLUMN users.role; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.role IS '用户角色：ADMIN（管理员）或USER（普通用户）';


--
-- Name: COLUMN users.parent_user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.parent_user_id IS 'Parent user ID when this user is a sub-account (SUB_USER); NULL for main accounts.';


--
-- Name: approval_log id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.approval_log ALTER COLUMN id SET DEFAULT nextval('public.approval_log_id_seq'::regclass);


--
-- Name: experiment_types id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.experiment_types ALTER COLUMN id SET DEFAULT nextval('public.experiment_types_id_seq'::regclass);


--
-- Name: image_attachments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.image_attachments ALTER COLUMN id SET DEFAULT nextval('public.image_attachments_id_seq'::regclass);


--
-- Name: images id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.images ALTER COLUMN id SET DEFAULT nextval('public.images_id_seq'::regclass);


--
-- Name: instruments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.instruments ALTER COLUMN id SET DEFAULT nextval('public.instruments_id_seq'::regclass);


--
-- Name: power_plants id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.power_plants ALTER COLUMN id SET DEFAULT nextval('public.power_plants_id_seq'::regclass);


--
-- Name: project_components id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.project_components ALTER COLUMN id SET DEFAULT nextval('public.project_components_id_seq'::regclass);


--
-- Name: project_instruments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.project_instruments ALTER COLUMN id SET DEFAULT nextval('public.project_instruments_id_seq'::regclass);


--
-- Name: projects id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.projects ALTER COLUMN id SET DEFAULT nextval('public.projects_id_seq'::regclass);


--
-- Name: report_items id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_items ALTER COLUMN id SET DEFAULT nextval('public.report_items_id_seq'::regclass);


--
-- Name: reports id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports ALTER COLUMN id SET DEFAULT nextval('public.reports_id_seq'::regclass);


--
-- Name: unit_components id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.unit_components ALTER COLUMN id SET DEFAULT nextval('public.unit_components_id_seq'::regclass);


--
-- Name: units id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.units ALTER COLUMN id SET DEFAULT nextval('public.units_id_seq'::regclass);


--
-- Data for Name: approval_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.approval_log (id, project_id, track, action, actor_name, created_at) FROM stdin;
1	19	ndt	submit	肖乐园	2026-02-03 08:52:04.27724
2	19	chem	submit	肖乐园	2026-02-03 08:52:06.974642
3	19	ndt	reject	肖乐园	2026-02-03 08:52:21.498793
4	19	chem	reject	肖乐园	2026-02-03 08:52:22.645093
5	19	ndt	submit	肖乐园	2026-02-03 08:52:23.922491
6	19	ndt	pass	肖乐园	2026-02-03 08:52:24.980859
7	19	ndt	reject	肖乐园	2026-02-03 08:52:30.323696
8	19	ndt	submit	肖乐园	2026-02-03 08:52:31.658277
9	19	ndt	pass	肖乐园	2026-02-03 08:52:32.597715
10	19	ndt	pass	肖乐园	2026-02-03 08:52:33.849211
11	19	chem	submit	肖乐园	2026-02-03 08:59:56.671796
12	19	chem	reject	肖乐园	2026-02-03 08:59:58.364155
13	19	chem	submit	肖乐园	2026-02-03 08:59:59.413741
14	19	chem	pass	肖乐园	2026-02-03 09:00:00.195658
15	19	chem	reject	肖乐园	2026-02-03 09:00:01.682593
16	19	chem	submit	肖乐园	2026-02-03 09:00:03.915817
17	19	chem	pass	肖乐园	2026-02-03 09:00:05.196704
18	19	chem	reject	肖乐园	2026-02-03 09:00:06.803915
19	19	chem	submit	肖乐园	2026-02-03 09:06:03.923845
20	19	chem	pass	肖乐园	2026-02-03 09:10:13.334007
21	19	chem	reject	肖乐园	2026-02-03 09:36:30.627187
22	19	chem	submit	肖乐园	2026-02-03 09:36:43.473688
23	19	chem	reject	肖乐园	2026-02-03 09:36:48.386926
24	19	chem	submit	肖乐园	2026-02-03 09:37:47.355066
25	19	chem	pass	肖乐园	2026-02-03 09:37:48.978878
26	19	chem	reject	肖乐园	2026-02-03 09:37:50.753349
27	19	chem	submit	肖乐园	2026-02-03 09:37:52.080328
28	19	chem	reject	肖乐园	2026-02-03 09:37:55.962736
29	19	chem	submit	肖乐园	2026-02-03 09:38:25.603865
30	19	chem	pass	肖乐园	2026-02-03 09:38:28.496921
31	19	chem	reject	肖乐园	2026-02-03 09:45:03.276896
32	19	chem	submit	肖乐园	2026-02-03 09:45:25.16732
33	19	chem	reject	肖乐园	2026-02-03 09:45:28.131937
34	19	chem	submit	肖乐园	2026-02-03 09:45:29.227896
35	19	chem	pass	肖乐园	2026-02-03 09:45:29.966066
36	19	chem	reject	肖乐园	2026-02-03 09:45:31.284616
37	19	chem	submit	肖乐园	2026-02-03 09:46:41.732703
38	19	chem	pass	肖乐园	2026-02-03 09:46:56.925426
39	19	chem	reject	肖乐园	2026-02-03 09:46:57.981804
40	19	chem	submit	肖乐园	2026-02-03 09:46:59.237997
41	19	chem	reject	肖乐园	2026-02-03 09:47:09.422269
42	19	chem	submit	肖乐园	2026-02-03 09:47:34.8551
43	19	chem	reject	肖乐园	2026-02-03 09:47:45.313119
44	19	chem	submit	肖乐园	2026-02-03 09:54:32.493935
45	19	chem	pass	肖乐园	2026-02-03 09:54:34.4675
46	19	chem	reject	肖乐园	2026-02-03 09:54:35.820543
47	19	chem	submit	肖乐园	2026-02-03 10:00:07.283141
48	19	chem	pass	肖乐园	2026-02-03 10:00:08.415221
49	19	chem	pass	肖乐园	2026-02-03 10:00:09.263584
50	20	ndt	submit	肖乐园	2026-02-03 10:02:14.220255
51	20	ndt	reject	肖乐园	2026-02-03 12:02:27.230325
52	20	ndt	submit	肖乐园	2026-02-03 14:13:24.036766
53	20	ndt	reject	肖乐园	2026-02-03 14:13:38.91784
54	20	ndt	submit	肖乐园	2026-02-03 14:17:44.699457
55	20	chem	submit	肖乐园	2026-02-03 14:18:42.578401
\.


--
-- Data for Name: experiment_types; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.experiment_types (id, name, code, table_schema, report_fields_schema, is_active) FROM stdin;
2	渗透检测	PT	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"起点位置","label":"起点位置","type":"text"},{"key":"终点位置","label":"终点位置","type":"text"},{"key":"长度","label":"长度","type":"text"},{"key":"级别","label":"级别","type":"text"},{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
23	内窥镜检测	VT	{"columns":[{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
24	涡流检测	ET	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"缺陷位置","label":"缺陷位置","type":"text"},{"key":"幅值","label":"幅值（dB）","type":"number"},{"key":"相位","label":"相位（°）","type":"number"},{"key":"减薄量","label":"减薄量","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
25	超声波测厚	UTT	{"columns":[{"key":"测点编号","label":"测点编号","type":"text"},{"key":"实测厚度","label":"实测厚度（mm）","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
1	磁粉检测	MT	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"起点位置","label":"起点位置","type":"text"},{"key":"终点位置","label":"终点位置","type":"text"},{"key":"长度","label":"长度","type":"text"},{"key":"级别","label":"级别","type":"text"},{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
27	氧化皮堆积检测	SOD	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"堆积量","label":"堆积量（％）","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
28	里氏硬度检测	LHD	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"1","label":"1","type":"number"},{"key":"2","label":"2","type":"number"},{"key":"3","label":"3","type":"number"},{"key":"4","label":"4","type":"number"},{"key":"5","label":"5","type":"number"},{"key":"平均","label":"平均","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
29	布什硬度检测	BHD	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"1","label":"1","type":"number"},{"key":"2","label":"2","type":"number"},{"key":"3","label":"3","type":"number"},{"key":"平均","label":"平均","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
30	金相检测	MET	{"columns":[{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
33	圆度测量	RDM	{"columns":[{"key":"弯头编号","label":"弯头编号","type":"text"},{"key":"公称直径","label":"公称直径（mm）","type":"number"},{"key":"弧面直径","label":"弧面直径（mm）","type":"number"},{"key":"侧面直径","label":"侧面直径（mm）","type":"number"},{"key":"测量圆度值","label":"测量圆度值","type":"number"},{"key":"允许圆度值","label":"允许圆度值","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
34	维氏硬度检测	VHN	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"1","label":"1","type":"number"},{"key":"2","label":"2","type":"number"},{"key":"3","label":"3","type":"number"},{"key":"平均","label":"平均","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
35	洛氏硬度检测	RHN	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"1","label":"1","type":"number"},{"key":"2","label":"2","type":"number"},{"key":"3","label":"3","type":"number"},{"key":"4","label":"4","type":"number"},{"key":"5","label":"5","type":"number"},{"key":"平均","label":"平均","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
3	超声检测	UT	{"columns":[{"key":"序号","label":"序号","type":"text"},{"key":"位置","label":"位置","type":"text"},{"key":"波幅","label":"波幅（dB）","type":"number"},{"key":"深度","label":"深度（mm）","type":"number"},{"key":"长度","label":"长度（mm）","type":"number"},{"key":"高度","label":"高度（mm）","type":"number"},{"key":"级别","label":"级别","type":"text"},{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
4	射线检测	RT	{"columns":[{"key":"序号","label":"序号","type":"text"},{"key":"焊接接头编号","label":"焊接接头编号","type":"text"},{"key":"底片编号","label":"底片编号","type":"text"},{"key":"黑度","label":"黑度","type":"text"},{"key":"厚度 mm","label":"厚度 mm","type":"text"},{"key":"识别丝号","label":"识别丝号","type":"text"},{"key":"缺陷位置、性质及数量","label":"缺陷位置、性质及数量","type":"text"},{"key":"评定级别","label":"评定级别","type":"text"},{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
26	管径测量	PDM	{"columns":[{"key":"测点编号","label":"测点编号","type":"text"},{"key":"实测管径","label":"实测管径（mm）","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
32	相控阵超声波检测	PAUT	{"columns":[{"key":"序号","label":"序号","type":"text"},{"key":"位置","label":"位置","type":"text"},{"key":"波幅","label":"波幅（dB）","type":"number"},{"key":"深度","label":"深度（mm）","type":"number"},{"key":"长度","label":"长度（mm）","type":"number"},{"key":"高度","label":"高度（mm）","type":"number"},{"key":"级别","label":"级别","type":"text"},{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
39	高温持久强度检测	HTC	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"断裂时间tu","label":"断裂时间tu/h","type":"number"},{"key":"断后伸长率A","label":"断后伸长率A/%","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
40	有效硬化层深度检测	CHD	{"columns":[{"key":"至边缘距离","label":"至边缘距离","type":"text"},{"key":"硬度","label":"硬度","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
38	高温拉伸检测	HTN	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"抗拉强度","label":"抗拉强度 R/MPa","type":"number"},{"key":"高温规定塑性延伸强度","label":"高温规定塑性延伸强度 R/MPa","type":"number"},{"key":"断后伸长率","label":"断后伸长率 A/%","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
36	冲击吸收能量检测	IMP	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"1","label":"1","type":"number"},{"key":"2","label":"2","type":"number"},{"key":"3","label":"3","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
37	室温拉伸检测	RTN	{"columns":[{"key":"编号","label":"编号","type":"text"},{"key":"抗拉强度Rm","label":"抗拉强度Rm/MPa","type":"number"},{"key":"下屈服强度或规定塑性延伸强度ReL或RP0.2","label":"下屈服强度或规定塑性延伸强度ReL或RP0.2/MPa","type":"number"},{"key":"断后伸长率A","label":"断后伸长率A/%","type":"number"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
31	合金分析检测	AAT	{"columns":[{"key":"编号","label":"编号","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
41	目视检测	VIS	{"columns":[{"key":"备注","label":"备注","type":"text"}]}	{"fields":[{"name":"serialNumber","label":"序号","type":"text","autoGenerate":true},{"name":"equipmentCategory","label":"设备类别","type":"text"},{"name":"equipmentName","label":"设备名称","type":"text"},{"name":"componentSpec","label":"部件规格","type":"text"},{"name":"instrumentModel","label":"仪器型号","type":"text"},{"name":"inspector","label":"检测人员","type":"text"},{"name":"location","label":"检测地点","type":"text"},{"name":"testDate","label":"检测日期","type":"date"}]}	t
\.


--
-- Data for Name: image_attachments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.image_attachments (id, report_id, image_urls, description, display_order, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: images; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.images (id, file_name, storage_path, file_size, mime_type, user_id, uploaded_at, report_id) FROM stdin;
1	图片1.png	.\\uploads\\ff0d7b59-f962-4490-9dee-5469600bd9de.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 11:12:18.28892	\N
2	工作票证明.jpg	.\\uploads\\d8f46825-e52c-49fc-a740-b9aed5c49f1f.jpg	505303	image/jpeg	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 11:12:28.377326	\N
3	图片1.png	.\\uploads\\4dd14f44-b5c0-4868-be23-983982b045a1.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 11:13:13.392525	\N
4	图片1.png	.\\uploads\\89f89221-f495-44b5-ac0c-7e0f48437c7a.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 11:15:36.303301	\N
5	图片1.png	.\\uploads\\14b9a191-b198-4d6b-b931-97bebcf7ad84.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 11:15:47.310853	\N
6	图片1.png	.\\uploads\\dc8b84b4-ae94-48b7-b7fc-9703c511bb12.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 11:16:17.128009	\N
7	图片1.png	.\\uploads\\550629dd-eecd-4c0f-889b-b6e59be13084.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 11:16:58.080682	\N
8	图片1.png	.\\uploads\\05e59649-ad07-4ea0-b7ab-bcbceb626a66.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 12:32:53.322194	\N
9	图片1.png	.\\uploads\\ffb75592-98d4-41a6-8e00-f48bf3b5d5d3.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 12:33:50.210641	\N
10	图片1.png	.\\uploads\\61309ab7-69c0-40de-a752-b602f4e41b75.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 12:52:16.692273	\N
11	工作票证明.jpg	.\\uploads\\e475cb37-ff2b-489a-a8f6-9679878fed1f.jpg	505303	image/jpeg	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 12:52:31.551931	\N
12	图片1.png	.\\uploads\\253e35b6-c26e-43db-bcbd-e344c0849587.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 14:46:25.574722	\N
13	图片1.png	.\\uploads\\6df20053-e77d-4b2a-b0c6-3e5011b24200.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 14:47:18.983765	\N
14	工作票证明.jpg	.\\uploads\\4dff5d32-0612-4659-aabc-50381d26e342.jpg	505303	image/jpeg	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 14:59:33.797665	\N
15	图片1.png	.\\uploads\\f8da8c05-bf6e-4a0f-a0a5-8167ceb64297.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 14:59:48.9594	\N
16	图片1.png	.\\uploads\\c1a9b9e8-ac70-40ff-8d49-98a024545cbb.png	289338	image/png	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-22 15:14:39.454613	\N
17	工作票证明.jpg	.\\uploads\\e5c635b4-bdaf-476a-8d01-4ca4edb64af7.jpg	505303	image/jpeg	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-10-31 11:33:07.628467	\N
18	20251126-141306.jpg	.\\uploads\\c1202dd0-1c42-48b8-94e0-1ceae5823392.jpg	219011	image/jpeg	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-11-27 09:12:26.709976	\N
19	20251126-141306.jpg	.\\uploads\\adfdd059-dde4-4995-bf06-40023f9d9351.jpg	219011	image/jpeg	a057308f-94f4-4bc9-9bf3-6831bbc18999	2025-11-27 09:12:38.248857	\N
20	20241205-152743021379.jpg	.\\uploads\\4c83d0e4-f5de-46a0-bea0-a6f458c34dc2.jpg	405447	image/jpeg	71af608e-6f75-4ef3-b732-40c3abbc252a	2026-01-27 11:01:48.215108	\N
21	前.jpg	.\\uploads\\ca5a37b2-4991-4e8d-9536-1f282cef601c.jpg	117949	image/jpeg	71af608e-6f75-4ef3-b732-40c3abbc252a	2026-02-02 10:22:24.747695	\N
22	前.jpg	.\\uploads\\c28fc42c-ed7c-426e-8b06-c9ccfc348aae.jpg	117949	image/jpeg	71af608e-6f75-4ef3-b732-40c3abbc252a	2026-02-02 10:23:31.734582	\N
\.


--
-- Data for Name: instruments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.instruments (id, instrument_name, instrument_model, instrument_number, created_at, updated_at) FROM stdin;
1	氧化皮检测仪	OMD-200	CL1-20Q012	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
2	现场金相显微镜	SY-01	CL1-20Q011	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
3	现场金相显微镜	SY-01	CL1-20Q010	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
5	里氏硬度计	BAMBINO2	CL2-20G005	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
6	里氏硬度计	BAMBINO2	CL2-20G006	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
7	里氏硬度计	BAMBINO2	CL2-20G007	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
8	里氏硬度计	BAMBINO2	CL2-20G008	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
9	里氏硬度计	BAMBINO2	CL2-20G009	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
10	手持式合金分析仪	X-MET8000 Optimum	CL1-20G002	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
11	手持式合金分析仪	X-MET8000 Optimum	CL1-20G003	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
12	标准负荷测力仪	HS2000-300A	CL2-20G001	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
13	管道应力分析软件	CAESAR II 2019	CL1-19Q019	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
14	电解腐蚀抛光仪	EP-06	CL1-19Q016	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
15	自动磨抛机	ECOMET30	CL1-19Q015	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
16	热压镶嵌机	MET4000	CL1-19Q014	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
17	手动砂轮切割机	MET250	CL1-19Q013	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
18	镀层测厚仪	OLYMPUS 38DL PLUS	CL1-19G017	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
19	镀层测厚仪	DeFelsko PosiTector 600	CL2-19G018	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
20	摆锤式冲击试验机	302D	CL1-19G012	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
21	磁轭探伤仪	1702	CL1-19G011	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
22	磁轭探伤仪	1702	CL1-19G010	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
23	磁轭探伤仪	1702	CL1-19G009	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
24	磁轭探伤仪	1702	CL1-19G008	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
25	磁轭探伤仪	1702	CL1-19G007	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
26	磁轭探伤仪	1702	CL1-19G006	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
27	自动显微维氏硬度计	VH1202	CL1-18G053	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
28	微机控制电子万能试验机	UTM5305HA	CL1-18G060	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
29	体视显微镜	M125C	CL1-18G061	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
30	数显布氏硬度计	BH3000	CL1-18G062	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
31	锤击布氏硬度计	PHB-1	CL2-18G031	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
32	锤击布氏硬度计	PHB-1	CL2-18G030	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
33	锤击布氏硬度计	PHB-1	CL2-18G029	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
34	锤击布氏硬度计	PHB-1	CL2-18G028	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
35	磁力数显布氏硬度计	PHB-200	CL1-18G032	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
36	红外热成像仪	F562	CL2-18Q066	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
37	红外热成像仪	F562	CL2-18Q065	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
38	红外热成像仪	F562	CL2-18Q064	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
39	红外热成像仪	F562	CL2-18Q063	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
40	C型布洛硬度计	PHBR-4	CL1-18G054	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
41	数显洛氏硬度计	574R	CL1-18G052	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
42	里氏硬度计	BAMBIN02	CL2-18G012	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
43	里氏硬度计	BAMBIN02	CL2-18G011	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
44	里氏硬度计	BAMBIN02	CL2-18G010	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
45	里氏硬度计	BAMBIN02	CL2-18G009	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
46	里氏硬度计	BAMBIN02	CL2-18G008	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
47	现场金相显微镜	XZD-500	CL1-18G025	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
48	现场金相显微镜	XZD-500	CL1-18G024	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
49	现场金相显微镜	XZD-500	CL1-18G023	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
50	现场金相显微镜	XZD-500	CL1-18G022	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
51	现场金相显微镜	XZD-500	CL1-18G021	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
52	手动磨抛	830	CL2-18Q059	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
53	手动磨抛	830	CL2-18Q058	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
54	手动磨抛	820	CL2-18Q057	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
55	手动磨抛	820	CL2-18Q056	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
56	台式倒置金相显微镜	DMi8C	CL1-18G055	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
57	便携型远场涡流探伤仪	EEC-39RFT	CL1-18G049	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
58	工业内窥镜	XLVUD84100	CL1-18Q051	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
59	工业内窥镜	XLLVB84100	CL1-18Q050	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
60	旋转磁场探伤仪	ZCM-DX1203A	CL2-18G046	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
61	旋转磁场探伤仪	ZCM-DX1203A	CL2-18G045	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
62	超声波探伤仪	HS700	CL1-18G039	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
63	超声波探伤仪	HS700	CL1-18G038	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
64	超声波探伤仪	HS700	CL1-18G037	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
65	超声波探伤仪	HS700	CL1-18G036	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
66	超声波探伤仪	HS700	CL1-18G035	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
67	交流磁粉探伤仪	LBNB-22016	CL2-18G044	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
68	交流磁粉探伤仪	LBNB-22016	CL2-18G043	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
69	交流磁粉探伤仪	LBNB-22016	CL2-18G042	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
70	交流磁粉探伤仪	LBNB-22016	CL2-18G041	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
71	交流磁粉探伤仪	LBNB-22016	CL2-18G040	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
72	波形显示型测厚仪	TS-ATG11	CL2-18G047	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
73	合金分析仪	X-MET8000	CL1-18G027	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
74	合金分析仪	X-MET8000	CL1-18G026	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
75	胶片冲洗装置	DL-P14A-NDT	CL1-18Q048	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
76	X射线探伤机	XXQ-3005	CL1-18G020	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
77	X射线探伤机	XXQ-3005	CL1-18G019	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
78	X射线探伤机	XXQ-2505	CL1-18G018	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
79	X射线探伤机	XXQ-2505	CL1-18G017	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
80	X射线探伤机	XXQ-2005	CL1-18G016	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
81	X射线探伤机	XXQ-2005	CL1-18G015	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
82	X射线探伤机	XXQ-2005	CL1-18G014	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
83	X射线探伤机	XXQ-2005	CL1-18G013	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
84	应力分析工作站	T7920	CL1-18Q034	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
85	应力分析工作站	T7920	CL1-18Q033	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
86	电阻应变仪	AFT-CM-10	CL2-18G006	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
87	辐射监测仪	R-PD	JS2-17G020	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
88	磁粉探伤仪	LKNB-22016	JS2-16G029	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
89	磁粉探伤仪	LKNB-22016	JS2-16G028	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
90	磁粉探伤仪	LKNB-22016	JS2-16G027	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
91	磁力数显布氏硬度计	PHB-200	JS1-16G031	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
92	C型布洛硬度计	PHBR-4-3	JS2-16G032	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
93	人工智能箱式电阻炉	SGM.M4/13AS	JS2-16Q033	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
94	相控阵检测探头	4L32	JS1-16Q013	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
95	相控阵检测探头	4L32	JS1-16Q012	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
96	多功能气体检测仪	PG610-P	JS2-16G003	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
97	多功能气体检测仪	PG610-P	JS2-16G002	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
98	超声波测厚仪	TIME2110	JS2-16G009	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
99	超声波测厚仪	TIME2110	JS2-16G008	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
100	超声波测厚仪	TIME2110	JS2-16G007	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
101	超声波测厚仪	TIME2110	JS2-16G006	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
102	超声波测厚仪	TIME2110	JS2-16G005	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
103	超声波测厚仪	TIME2110	JS2-16G004	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
104	超声波探伤仪	HS620	JS1-16G011	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
105	超声波探伤仪	HS620	JS1-16G010	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
106	工业内窥镜	GT200A S252	JS1-15Q044	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
107	工业内窥镜	GT200A S252	JS1-15Q043	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
108	电子拉力计	EDX-10T	JS1-15G042	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
109	电子拉力计	EDX-5T	JS1-15G041	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
110	交流磁粉探伤仪	MY-2	JS2-15G024	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
111	交流磁粉探伤仪	MY-2	JS2-15G023	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
112	交流磁粉探伤仪	MY-2	JS2-15G022	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
113	交流磁粉探伤仪	MY-2	JS2-15G021	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
114	交流磁粉探伤仪	MY-2	JS2-15G020	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
115	交流磁粉探伤仪	MY-2	JS2-15G019	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
116	超声波测厚仪	MT-160	JS2-15G018	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
117	超声波测厚仪	MT-160	JS2-15G017	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
118	超声波测厚仪	MT-160	JS2-15G016	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
119	超声波测厚仪	MT-160	JS2-15G015	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
120	超声波测厚仪	MT-160	JS2-15G014	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
121	超声波测厚仪	MT-160	JS2-15G013	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
122	超声波测厚仪	MT-160	JS2-15G012	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
123	超声波测厚仪	MT-160	JS2-15G011	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
124	里氏硬度计	Bambino2	JS2-15G010	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
125	里氏硬度计	Bambino2	JS2-15G009	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
126	里氏硬度计	Bambino2	JS2-15G006	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
127	里氏硬度计	Bambino2	JS2-15G005	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
128	里氏硬度计	Bambino2	JS2-15G004	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
129	合金分析仪	X-MET8000	JS1-15G039	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
130	安全阀在线校验仪	JY-S	JS1-15G003	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
131	超声相控阵检测仪	SUPOR-32P	JS1-14G020	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
132	磁粉探伤仪	CY-1000	JS2-14G024	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
133	超声波探伤仪	CTS-9006PLUS	JS1-14G023	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
134	超声波探伤仪	CTS-9006PLUS	JS1-14G022	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
135	超声波探伤仪	CTS-9006PLUS	JS1-14G021	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
136	超声波测厚仪	MT-160	JS2-14G009	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
137	超声波测厚仪	MT-160	JS2-14G008	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
138	超声波测厚仪	MT-160	JS2-14G007	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
139	超声波测厚仪	MT-160	JS2-14G006	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
140	超声波测厚仪	MT-160	JS2-14G005	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
141	超声波测厚仪	MT-160	JS2-14G004	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
142	现场金相显微镜	JXD-900	JS1-14Q003	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
143	电子拉力计	EDIR-10T	JS1-14G002	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
144	电子拉力计	EDIR-5T	JS2-14G001	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
145	手持式合金分析仪	Niton XL3t980	JS1-13G012	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
146	相控阵检测仪	PHSCAN32/64	JS1-13Q002	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
147	氧化皮检测仪	OMD-100	JS1-12Q030	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
148	工业内窥镜	WIWA ES325	JS1-12Q029	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
149	安全阀在线校验仪	YLT-DL-S	JS1-12Q033	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
150	便携式智能超声波检测系统	Isonic2005	JS1-07G175	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
151	超声波硬度计	newsonic	JS1-14G017	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
152	磁粉探伤仪	MY-2	JS2-14G014	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
153	磁粉探伤仪	MY-2	JS2-14G013	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
154	磁粉探伤仪	MY-2	JS2-14G012	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
155	磁粉探伤仪	MY-2	JS2-14G011	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
156	磁粉探伤仪	MY-2	JS2-14G010	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
157	红外热成像仪	Fluke Ti125	JS1-14Q019	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
158	红外热成像仪	Fluke Ti125	JS1-14Q018	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
159	16晶片相控阵检测探头	2L16	JS1-12Q035	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
160	安全阀动态研磨机	SFX-150I	JS1-12Q032	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
161	安全阀定压校验台	SAT-Q32S	JS1-12Q031	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
162	试压泵	DSY-10MPa	GJ2-06G26	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
163	全站仪	RTS238	GJ2-06G24	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
164	便携式可燃气体检测仪	SNE168	GJ2-06G25	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
165	相控阵超声检测开发平台		CL1-19Q002	2025-12-30 11:21:05.833525	2025-12-30 11:21:05.833525
\.


--
-- Data for Name: power_plants; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.power_plants (id, name, region, short_name, province, city, address, phone, fax, remark, created_at, updated_at) FROM stdin;
6	华润电力(沈阳)有限公司	东北大区	\N	辽宁省	沈阳市	待补充	\N	\N	\N	2025-12-25 11:42:26.334668	2025-12-25 11:42:26.334668
7	华润电力(盘锦)有限公司	东北大区	\N	辽宁省	盘锦市	待补充	\N	\N	\N	2025-12-25 11:42:26.796378	2025-12-25 11:42:26.796378
8	华润电力(锦州)有限公司	东北大区	\N	辽宁省	锦州市	待补充	\N	\N	\N	2025-12-25 11:42:26.836382	2025-12-25 11:42:26.836382
9	湖南华润电力鲤鱼江有限公司	华南大区	\N	湖南省	郴州市	待补充	\N	\N	\N	2025-12-25 11:42:26.874365	2025-12-25 11:42:26.874365
10	深圳市深汕特别合作区华润电力有限公司	华南大区	\N	广东省	深圳市	待补充	\N	\N	\N	2025-12-25 11:42:26.907361	2025-12-25 11:42:26.907361
11	广州华润热电有限公司	华南大区	\N	广东省	广州市	待补充	\N	\N	\N	2025-12-25 11:42:26.937364	2025-12-25 11:42:26.937364
12	华润电力(贺州)有限公司	华南大区	\N	广西壮族自治区	贺州市	待补充	\N	\N	\N	2025-12-25 11:42:26.971365	2025-12-25 11:42:26.971365
13	华润电力(沧州)有限公司	华北大区	\N	河北省	沧州市	待补充	\N	\N	\N	2025-12-25 11:42:26.995364	2025-12-25 11:42:26.995364
14	华润电力唐山丰润有限公司	华北大区	\N	河北省	唐山市	待补充	\N	\N	\N	2025-12-25 11:42:27.024366	2025-12-25 11:42:27.024366
15	华润电力(渤海新区)有限公司	华北大区	\N	河北省	沧州市	待补充	\N	\N	\N	2025-12-25 11:42:27.049367	2025-12-25 11:42:27.049367
16	华润电力(唐山曹妃甸)有限公司	华北大区	\N	河北省	唐山市	待补充	\N	\N	\N	2025-12-25 11:42:27.067362	2025-12-25 11:42:27.067362
17	华润电力(沧州运东)有限公司	华北大区	\N	河北省	沧州市	待补充	\N	\N	\N	2025-12-25 11:42:27.087364	2025-12-25 11:42:27.087364
18	华润电力(北京)有限公司	华北大区	\N	北京市	朝阳区	待补充	\N	\N	\N	2025-12-25 11:42:27.107365	2025-12-25 11:42:27.107365
20	徐州华润电力有限公司	华东大区	\N	江苏省	徐州市	待补充	\N	\N	\N	2025-12-25 11:42:27.154363	2025-12-25 11:42:27.154363
21	南京华润热电有限公司	华东大区	\N	江苏省	南京市	待补充	\N	\N	\N	2025-12-25 11:42:27.18936	2025-12-25 11:42:27.18936
22	南京化学工业园热电有限公司	华东大区	\N	江苏省	南京市	待补充	\N	\N	\N	2025-12-25 11:42:27.265365	2025-12-25 11:42:27.265365
23	徐州华鑫发电有限公司	华东大区	\N	江苏省	徐州市	待补充	\N	\N	\N	2025-12-25 11:42:27.343361	2025-12-25 11:42:27.343361
24	华润电力(镇江)有限公司	华东大区	\N	江苏省	镇江市	待补充	\N	\N	\N	2025-12-25 11:42:27.406204	2025-12-25 11:42:27.406204
25	华润电力(常熟)有限公司	华东大区	\N	江苏省	常熟市	待补充	\N	\N	\N	2025-12-25 11:42:27.485526	2025-12-25 11:42:27.485526
26	江苏南热发电有限责任公司	华东大区	\N	江苏省	南京市	待补充	\N	\N	\N	2025-12-25 11:42:27.523528	2025-12-25 11:42:27.523528
27	华润电力(阜阳)有限公司	华东大区	\N	安徽省	阜阳市	待补充	\N	\N	\N	2025-12-25 11:42:27.552527	2025-12-25 11:42:27.552527
28	华润电力(温州)有限公司	华东大区	\N	浙江省	温州市	待补充	\N	\N	\N	2025-12-25 11:42:27.589523	2025-12-25 11:42:27.589523
29	华润电力(登封)有限公司	中西大区	\N	河南省	登封市	待补充	\N	\N	\N	2025-12-25 11:42:27.625525	2025-12-25 11:42:27.625525
30	华润电力(古城)有限公司	中西大区	\N	河南省	驻马店市	待补充	\N	\N	\N	2025-12-25 11:42:27.666527	2025-12-25 11:42:27.666527
31	华润电力(焦作)有限公司	中西大区	\N	河南省	焦作市	待补充	\N	\N	\N	2025-12-25 11:42:27.688465	2025-12-25 11:42:27.688465
32	华润电力(首阳山)有限公司	中西大区	\N	河南省	洛阳市	待补充	\N	\N	\N	2025-12-25 11:42:27.722523	2025-12-25 11:42:27.722523
33	华润电力(湖北)有限公司	华中大区	\N	湖北省	赤壁市	待补充	\N	\N	\N	2025-12-25 11:42:27.738522	2025-12-25 11:42:27.738522
34	华润电力(宜昌)有限公司	华中大区	\N	湖北省	宜昌市	待补充	\N	\N	\N	2025-12-25 11:42:27.753528	2025-12-25 11:42:27.753528
35	华润电力(锡林郭勒)有限公司	北方大区	\N	内蒙古自治区	锡林郭勒盟	待补充	\N	\N	\N	2025-12-25 11:42:27.769523	2025-12-25 11:42:27.769523
36	华润电力(磴口)有限公司	北方大区	\N	内蒙古自治区	巴彦淖尔市	待补充	\N	\N	\N	2025-12-25 11:42:27.787284	2025-12-25 11:42:27.787284
19	华润电力(菏泽)有限公司	华北大区	\N	山东	菏泽市	山东省菏泽市牡丹区皇镇街道西王楼附近	\N	\N	\N	2025-12-25 11:42:27.129362	2026-01-27 10:58:38.313697
\.


--
-- Data for Name: project_components; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.project_components (id, project_id, component_name, material, category, pipe_diameter, wall_thickness, created_at, updated_at, remark) FROM stdin;
\.


--
-- Data for Name: project_instruments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.project_instruments (id, project_id, instrument_name, instrument_model, instrument_number, created_at, updated_at, global_instrument_id, is_default, experiment_type_code) FROM stdin;
\.


--
-- Data for Name: projects; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.projects (id, project_number, third_party_project_number, third_party_name, project_name, customer_name, user_id, start_date, end_date, status, description, selected_experiment_type_ids, created_at, updated_at, responsible_person, reviewer_ndt, review_date_ndt, approver_ndt, approval_date_ndt, staff, reviewer_chem, review_date_chem, approver_chem, approval_date_chem, writer_ndt, writer_date_ndt, writer_chem, writer_date_chem, customer, customer_contact, power_plant_id, unit_id, approval_step_ndt, approval_step_chem, rejection_step_ndt, rejection_step_chem) FROM stdin;
\.


--
-- Data for Name: report_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.report_items (id, report_id, experiment_type_id, table_data, summary, created_at) FROM stdin;
\.


--
-- Data for Name: reports; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.reports (id, project_id, title, report_number, user_id, experiment_type_id, project_component_id, inspector, test_method, equipment, test_standard, component_name, equipment_category, equipment_name, component_spec, instrument_model, instrument_number, test_date, location, report_image, has_defect, status, custom_fields, created_at, updated_at, project_instrument_id, detection_content) FROM stdin;
\.


--
-- Data for Name: unit_components; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.unit_components (id, unit_id, component_name, material, category, pipe_diameter, wall_thickness, remark, created_at, updated_at) FROM stdin;
3	45	高温再热蒸汽管道	07Cr18Ni11Nb	\N	10	12	\N	2025-12-29 14:49:22.886096	2025-12-29 14:49:22.886096
4	45	11	\N	11	11	1	\N	2026-01-04 15:38:05.203637	2026-01-04 15:38:05.203637
5	14	主蒸汽管道	\N	\N	\N	\N	\N	2026-01-19 11:15:35.097671	2026-01-19 11:15:35.097671
\.


--
-- Data for Name: units; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.units (id, power_plant_id, unit_name, unit_number, remark, created_at, updated_at, installed_capacity) FROM stdin;
2	6	1	1	\N	2025-12-25 11:42:26.702634	2025-12-25 11:42:26.702634	\N
3	6	2	2	\N	2025-12-25 11:42:26.737633	2025-12-25 11:42:26.737633	\N
4	6	3	3	\N	2025-12-25 11:42:26.748633	2025-12-25 11:42:26.748633	\N
5	6	4	4	\N	2025-12-25 11:42:26.75963	2025-12-25 11:42:26.75963	\N
6	6	5	5	\N	2025-12-25 11:42:26.770631	2025-12-25 11:42:26.770631	\N
7	6	6	6	\N	2025-12-25 11:42:26.782282	2025-12-25 11:42:26.782282	\N
8	7	1	1	\N	2025-12-25 11:42:26.804365	2025-12-25 11:42:26.804365	\N
9	7	2	2	\N	2025-12-25 11:42:26.809367	2025-12-25 11:42:26.809367	\N
10	7	3	3	\N	2025-12-25 11:42:26.815367	2025-12-25 11:42:26.815367	\N
11	7	4	4	\N	2025-12-25 11:42:26.819362	2025-12-25 11:42:26.819362	\N
12	7	5	5	\N	2025-12-25 11:42:26.825366	2025-12-25 11:42:26.825366	\N
13	7	6	6	\N	2025-12-25 11:42:26.829367	2025-12-25 11:42:26.829367	\N
14	8	1	1	\N	2025-12-25 11:42:26.848361	2025-12-25 11:42:26.848361	\N
15	8	2	2	\N	2025-12-25 11:42:26.852361	2025-12-25 11:42:26.852361	\N
16	8	3	3	\N	2025-12-25 11:42:26.856362	2025-12-25 11:42:26.856362	\N
17	8	4	4	\N	2025-12-25 11:42:26.860377	2025-12-25 11:42:26.860377	\N
18	8	5	5	\N	2025-12-25 11:42:26.863364	2025-12-25 11:42:26.863364	\N
19	8	6	6	\N	2025-12-25 11:42:26.867361	2025-12-25 11:42:26.867361	\N
20	9	1	1	\N	2025-12-25 11:42:26.881306	2025-12-25 11:42:26.881306	\N
21	9	2	2	\N	2025-12-25 11:42:26.885362	2025-12-25 11:42:26.885362	\N
22	9	3	3	\N	2025-12-25 11:42:26.889364	2025-12-25 11:42:26.889364	\N
23	9	4	4	\N	2025-12-25 11:42:26.894309	2025-12-25 11:42:26.894309	\N
24	9	5	5	\N	2025-12-25 11:42:26.897377	2025-12-25 11:42:26.897377	\N
25	9	6	6	\N	2025-12-25 11:42:26.901362	2025-12-25 11:42:26.901362	\N
26	10	1	1	\N	2025-12-25 11:42:26.914364	2025-12-25 11:42:26.914364	\N
27	10	2	2	\N	2025-12-25 11:42:26.918364	2025-12-25 11:42:26.918364	\N
28	10	3	3	\N	2025-12-25 11:42:26.921363	2025-12-25 11:42:26.921363	\N
29	10	4	4	\N	2025-12-25 11:42:26.924366	2025-12-25 11:42:26.924366	\N
30	10	5	5	\N	2025-12-25 11:42:26.928364	2025-12-25 11:42:26.928364	\N
31	10	6	6	\N	2025-12-25 11:42:26.931361	2025-12-25 11:42:26.931361	\N
32	11	1	1	\N	2025-12-25 11:42:26.948363	2025-12-25 11:42:26.948363	\N
33	11	2	2	\N	2025-12-25 11:42:26.951364	2025-12-25 11:42:26.951364	\N
34	11	3	3	\N	2025-12-25 11:42:26.954371	2025-12-25 11:42:26.954371	\N
35	11	4	4	\N	2025-12-25 11:42:26.958363	2025-12-25 11:42:26.958363	\N
36	11	5	5	\N	2025-12-25 11:42:26.961364	2025-12-25 11:42:26.961364	\N
37	11	6	6	\N	2025-12-25 11:42:26.965363	2025-12-25 11:42:26.965363	\N
38	12	1	1	\N	2025-12-25 11:42:26.975363	2025-12-25 11:42:26.975363	\N
39	12	2	2	\N	2025-12-25 11:42:26.978363	2025-12-25 11:42:26.978363	\N
40	12	3	3	\N	2025-12-25 11:42:26.981363	2025-12-25 11:42:26.981363	\N
41	12	4	4	\N	2025-12-25 11:42:26.984362	2025-12-25 11:42:26.984362	\N
42	12	5	5	\N	2025-12-25 11:42:26.987364	2025-12-25 11:42:26.987364	\N
43	12	6	6	\N	2025-12-25 11:42:26.990364	2025-12-25 11:42:26.990364	\N
44	13	1	1	\N	2025-12-25 11:42:27.004363	2025-12-25 11:42:27.004363	\N
45	13	2	2	\N	2025-12-25 11:42:27.007364	2025-12-25 11:42:27.007364	\N
50	14	1	1	\N	2025-12-25 11:42:27.028365	2025-12-25 11:42:27.028365	\N
51	14	2	2	\N	2025-12-25 11:42:27.031377	2025-12-25 11:42:27.031377	\N
52	14	3	3	\N	2025-12-25 11:42:27.034373	2025-12-25 11:42:27.034373	\N
53	14	4	4	\N	2025-12-25 11:42:27.038362	2025-12-25 11:42:27.038362	\N
54	14	5	5	\N	2025-12-25 11:42:27.041381	2025-12-25 11:42:27.041381	\N
55	14	6	6	\N	2025-12-25 11:42:27.044368	2025-12-25 11:42:27.044368	\N
56	15	1	1	\N	2025-12-25 11:42:27.052365	2025-12-25 11:42:27.052365	\N
57	15	2	2	\N	2025-12-25 11:42:27.05437	2025-12-25 11:42:27.05437	\N
58	15	3	3	\N	2025-12-25 11:42:27.056364	2025-12-25 11:42:27.056364	\N
59	15	4	4	\N	2025-12-25 11:42:27.058364	2025-12-25 11:42:27.058364	\N
60	15	5	5	\N	2025-12-25 11:42:27.060369	2025-12-25 11:42:27.060369	\N
61	15	6	6	\N	2025-12-25 11:42:27.063362	2025-12-25 11:42:27.063362	\N
62	16	1	1	\N	2025-12-25 11:42:27.069365	2025-12-25 11:42:27.069365	\N
63	16	2	2	\N	2025-12-25 11:42:27.071364	2025-12-25 11:42:27.071364	\N
64	16	3	3	\N	2025-12-25 11:42:27.073363	2025-12-25 11:42:27.073363	\N
65	16	4	4	\N	2025-12-25 11:42:27.076361	2025-12-25 11:42:27.076361	\N
66	16	5	5	\N	2025-12-25 11:42:27.079362	2025-12-25 11:42:27.079362	\N
67	16	6	6	\N	2025-12-25 11:42:27.082362	2025-12-25 11:42:27.082362	\N
68	17	1	1	\N	2025-12-25 11:42:27.090363	2025-12-25 11:42:27.090363	\N
69	17	2	2	\N	2025-12-25 11:42:27.093362	2025-12-25 11:42:27.093362	\N
70	17	3	3	\N	2025-12-25 11:42:27.096364	2025-12-25 11:42:27.096364	\N
71	17	4	4	\N	2025-12-25 11:42:27.099366	2025-12-25 11:42:27.099366	\N
72	17	5	5	\N	2025-12-25 11:42:27.101364	2025-12-25 11:42:27.101364	\N
73	17	6	6	\N	2025-12-25 11:42:27.10336	2025-12-25 11:42:27.10336	\N
74	18	1	1	\N	2025-12-25 11:42:27.109364	2025-12-25 11:42:27.109364	\N
75	18	2	2	\N	2025-12-25 11:42:27.111363	2025-12-25 11:42:27.111363	\N
76	18	3	3	\N	2025-12-25 11:42:27.115362	2025-12-25 11:42:27.115362	\N
77	18	4	4	\N	2025-12-25 11:42:27.118362	2025-12-25 11:42:27.118362	\N
78	18	5	5	\N	2025-12-25 11:42:27.120362	2025-12-25 11:42:27.120362	\N
79	18	6	6	\N	2025-12-25 11:42:27.123361	2025-12-25 11:42:27.123361	\N
80	19	1	1	\N	2025-12-25 11:42:27.132362	2025-12-25 11:42:27.132362	\N
81	19	2	2	\N	2025-12-25 11:42:27.135365	2025-12-25 11:42:27.135365	\N
82	19	3	3	\N	2025-12-25 11:42:27.13836	2025-12-25 11:42:27.13836	\N
83	19	4	4	\N	2025-12-25 11:42:27.141367	2025-12-25 11:42:27.141367	\N
84	19	5	5	\N	2025-12-25 11:42:27.145364	2025-12-25 11:42:27.145364	\N
85	19	6	6	\N	2025-12-25 11:42:27.148362	2025-12-25 11:42:27.148362	\N
86	20	1	1	\N	2025-12-25 11:42:27.157365	2025-12-25 11:42:27.157365	\N
87	20	2	2	\N	2025-12-25 11:42:27.161366	2025-12-25 11:42:27.161366	\N
88	20	3	3	\N	2025-12-25 11:42:27.166362	2025-12-25 11:42:27.166362	\N
89	20	4	4	\N	2025-12-25 11:42:27.171363	2025-12-25 11:42:27.171363	\N
90	20	5	5	\N	2025-12-25 11:42:27.176363	2025-12-25 11:42:27.176363	\N
91	20	6	6	\N	2025-12-25 11:42:27.181364	2025-12-25 11:42:27.181364	\N
92	21	1	1	\N	2025-12-25 11:42:27.204364	2025-12-25 11:42:27.204364	\N
93	21	2	2	\N	2025-12-25 11:42:27.215361	2025-12-25 11:42:27.215361	\N
94	21	3	3	\N	2025-12-25 11:42:27.226351	2025-12-25 11:42:27.226351	\N
95	21	4	4	\N	2025-12-25 11:42:27.232362	2025-12-25 11:42:27.232362	\N
96	21	5	5	\N	2025-12-25 11:42:27.238307	2025-12-25 11:42:27.238307	\N
97	21	6	6	\N	2025-12-25 11:42:27.244366	2025-12-25 11:42:27.244366	\N
98	22	1	1	\N	2025-12-25 11:42:27.271363	2025-12-25 11:42:27.271363	\N
99	22	2	2	\N	2025-12-25 11:42:27.282364	2025-12-25 11:42:27.282364	\N
100	22	3	3	\N	2025-12-25 11:42:27.293361	2025-12-25 11:42:27.293361	\N
101	22	4	4	\N	2025-12-25 11:42:27.304366	2025-12-25 11:42:27.304366	\N
102	22	5	5	\N	2025-12-25 11:42:27.315361	2025-12-25 11:42:27.315361	\N
103	22	6	6	\N	2025-12-25 11:42:27.32636	2025-12-25 11:42:27.32636	\N
104	23	1	1	\N	2025-12-25 11:42:27.350308	2025-12-25 11:42:27.350308	\N
105	23	2	2	\N	2025-12-25 11:42:27.358363	2025-12-25 11:42:27.358363	\N
106	23	3	3	\N	2025-12-25 11:42:27.371366	2025-12-25 11:42:27.371366	\N
107	23	4	4	\N	2025-12-25 11:42:27.382201	2025-12-25 11:42:27.382201	\N
108	23	5	5	\N	2025-12-25 11:42:27.393201	2025-12-25 11:42:27.393201	\N
109	23	6	6	\N	2025-12-25 11:42:27.399201	2025-12-25 11:42:27.399201	\N
110	24	1	1	\N	2025-12-25 11:42:27.4162	2025-12-25 11:42:27.4162	\N
111	24	2	2	\N	2025-12-25 11:42:27.427444	2025-12-25 11:42:27.427444	\N
112	24	3	3	\N	2025-12-25 11:42:27.439524	2025-12-25 11:42:27.439524	\N
113	24	4	4	\N	2025-12-25 11:42:27.449523	2025-12-25 11:42:27.449523	\N
114	24	5	5	\N	2025-12-25 11:42:27.460526	2025-12-25 11:42:27.460526	\N
115	24	6	6	\N	2025-12-25 11:42:27.470525	2025-12-25 11:42:27.470525	\N
116	25	1	1	\N	2025-12-25 11:42:27.492526	2025-12-25 11:42:27.492526	\N
117	25	2	2	\N	2025-12-25 11:42:27.497526	2025-12-25 11:42:27.497526	\N
118	25	3	3	\N	2025-12-25 11:42:27.501528	2025-12-25 11:42:27.501528	\N
119	25	4	4	\N	2025-12-25 11:42:27.505526	2025-12-25 11:42:27.505526	\N
120	25	5	5	\N	2025-12-25 11:42:27.508524	2025-12-25 11:42:27.508524	\N
121	25	6	6	\N	2025-12-25 11:42:27.514536	2025-12-25 11:42:27.514536	\N
122	26	1	1	\N	2025-12-25 11:42:27.527527	2025-12-25 11:42:27.527527	\N
123	26	2	2	\N	2025-12-25 11:42:27.531522	2025-12-25 11:42:27.531522	\N
124	26	3	3	\N	2025-12-25 11:42:27.535532	2025-12-25 11:42:27.535532	\N
125	26	4	4	\N	2025-12-25 11:42:27.539525	2025-12-25 11:42:27.539525	\N
126	26	5	5	\N	2025-12-25 11:42:27.542529	2025-12-25 11:42:27.542529	\N
127	26	6	6	\N	2025-12-25 11:42:27.546523	2025-12-25 11:42:27.546523	\N
128	27	1	1	\N	2025-12-25 11:42:27.555535	2025-12-25 11:42:27.555535	\N
129	27	2	2	\N	2025-12-25 11:42:27.558525	2025-12-25 11:42:27.558525	\N
130	27	3	3	\N	2025-12-25 11:42:27.570524	2025-12-25 11:42:27.570524	\N
131	27	4	4	\N	2025-12-25 11:42:27.574527	2025-12-25 11:42:27.574527	\N
132	27	5	5	\N	2025-12-25 11:42:27.579524	2025-12-25 11:42:27.579524	\N
133	27	6	6	\N	2025-12-25 11:42:27.582528	2025-12-25 11:42:27.582528	\N
134	28	1	1	\N	2025-12-25 11:42:27.594524	2025-12-25 11:42:27.594524	\N
135	28	2	2	\N	2025-12-25 11:42:27.598522	2025-12-25 11:42:27.598522	\N
136	28	3	3	\N	2025-12-25 11:42:27.602523	2025-12-25 11:42:27.602523	\N
137	28	4	4	\N	2025-12-25 11:42:27.607525	2025-12-25 11:42:27.607525	\N
138	28	5	5	\N	2025-12-25 11:42:27.612526	2025-12-25 11:42:27.612526	\N
139	28	6	6	\N	2025-12-25 11:42:27.617522	2025-12-25 11:42:27.617522	\N
140	29	1	1	\N	2025-12-25 11:42:27.629522	2025-12-25 11:42:27.629522	\N
141	29	2	2	\N	2025-12-25 11:42:27.635525	2025-12-25 11:42:27.635525	\N
142	29	3	3	\N	2025-12-25 11:42:27.640529	2025-12-25 11:42:27.640529	\N
143	29	4	4	\N	2025-12-25 11:42:27.648526	2025-12-25 11:42:27.648526	\N
144	29	5	5	\N	2025-12-25 11:42:27.654525	2025-12-25 11:42:27.654525	\N
145	29	6	6	\N	2025-12-25 11:42:27.659524	2025-12-25 11:42:27.659524	\N
146	30	1	1	\N	2025-12-25 11:42:27.669524	2025-12-25 11:42:27.669524	\N
147	30	2	2	\N	2025-12-25 11:42:27.672522	2025-12-25 11:42:27.672522	\N
148	30	3	3	\N	2025-12-25 11:42:27.674523	2025-12-25 11:42:27.674523	\N
149	30	4	4	\N	2025-12-25 11:42:27.677523	2025-12-25 11:42:27.677523	\N
150	30	5	5	\N	2025-12-25 11:42:27.681525	2025-12-25 11:42:27.681525	\N
151	30	6	6	\N	2025-12-25 11:42:27.683522	2025-12-25 11:42:27.683522	\N
152	31	1	1	\N	2025-12-25 11:42:27.703524	2025-12-25 11:42:27.703524	\N
153	31	2	2	\N	2025-12-25 11:42:27.707524	2025-12-25 11:42:27.707524	\N
154	31	3	3	\N	2025-12-25 11:42:27.709525	2025-12-25 11:42:27.709525	\N
155	31	4	4	\N	2025-12-25 11:42:27.712523	2025-12-25 11:42:27.712523	\N
156	31	5	5	\N	2025-12-25 11:42:27.715523	2025-12-25 11:42:27.715523	\N
157	31	6	6	\N	2025-12-25 11:42:27.718524	2025-12-25 11:42:27.718524	\N
158	32	1	1	\N	2025-12-25 11:42:27.724526	2025-12-25 11:42:27.724526	\N
159	32	2	2	\N	2025-12-25 11:42:27.726529	2025-12-25 11:42:27.726529	\N
160	32	3	3	\N	2025-12-25 11:42:27.728524	2025-12-25 11:42:27.728524	\N
161	32	4	4	\N	2025-12-25 11:42:27.730524	2025-12-25 11:42:27.730524	\N
162	32	5	5	\N	2025-12-25 11:42:27.732524	2025-12-25 11:42:27.732524	\N
163	32	6	6	\N	2025-12-25 11:42:27.734523	2025-12-25 11:42:27.734523	\N
164	33	1	1	\N	2025-12-25 11:42:27.740525	2025-12-25 11:42:27.740525	\N
165	33	2	2	\N	2025-12-25 11:42:27.742525	2025-12-25 11:42:27.742525	\N
166	33	3	3	\N	2025-12-25 11:42:27.744523	2025-12-25 11:42:27.744523	\N
167	33	4	4	\N	2025-12-25 11:42:27.746525	2025-12-25 11:42:27.746525	\N
168	33	5	5	\N	2025-12-25 11:42:27.747525	2025-12-25 11:42:27.747525	\N
169	33	6	6	\N	2025-12-25 11:42:27.749522	2025-12-25 11:42:27.749522	\N
170	34	1	1	\N	2025-12-25 11:42:27.755526	2025-12-25 11:42:27.755526	\N
171	34	2	2	\N	2025-12-25 11:42:27.757524	2025-12-25 11:42:27.757524	\N
172	34	3	3	\N	2025-12-25 11:42:27.759525	2025-12-25 11:42:27.759525	\N
173	34	4	4	\N	2025-12-25 11:42:27.761525	2025-12-25 11:42:27.761525	\N
174	34	5	5	\N	2025-12-25 11:42:27.763525	2025-12-25 11:42:27.763525	\N
175	34	6	6	\N	2025-12-25 11:42:27.765537	2025-12-25 11:42:27.765537	\N
176	35	1	1	\N	2025-12-25 11:42:27.771468	2025-12-25 11:42:27.771468	\N
177	35	2	2	\N	2025-12-25 11:42:27.773879	2025-12-25 11:42:27.773879	\N
178	35	3	3	\N	2025-12-25 11:42:27.776277	2025-12-25 11:42:27.776277	\N
179	35	4	4	\N	2025-12-25 11:42:27.778281	2025-12-25 11:42:27.778281	\N
180	35	5	5	\N	2025-12-25 11:42:27.780275	2025-12-25 11:42:27.780275	\N
181	35	6	6	\N	2025-12-25 11:42:27.783279	2025-12-25 11:42:27.783279	\N
182	36	1	1	\N	2025-12-25 11:42:27.790288	2025-12-25 11:42:27.790288	\N
183	36	2	2	\N	2025-12-25 11:42:27.792282	2025-12-25 11:42:27.792282	\N
184	36	3	3	\N	2025-12-25 11:42:27.795279	2025-12-25 11:42:27.795279	\N
185	36	4	4	\N	2025-12-25 11:42:27.798277	2025-12-25 11:42:27.798277	\N
186	36	5	5	\N	2025-12-25 11:42:27.801282	2025-12-25 11:42:27.801282	\N
187	36	6	6	\N	2025-12-25 11:42:27.803278	2025-12-25 11:42:27.803278	\N
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, normalized_username, email, normalized_email, email_confirmed, password_hash, security_stamp, concurrency_stamp, phone_number, phone_number_confirmed, two_factor_enabled, lockout_end, lockout_enabled, access_failed_count, full_name, department, created_at, role, parent_user_id) FROM stdin;
a057308f-94f4-4bc9-9bf3-6831bbc18999	admin	ADMIN		\N	t	$2a$10$lcbRtbYsReg.yYdcct88YezPhLmofJ0z98GaLToImJPqtySTJgsLy	6e1ab9dc-bebd-43c5-85c3-9ad48da27c29	ce4820b6-0374-4ef2-a95b-247ee9fe64c7	\N	f	f	\N	t	0	系统管理员		2025-10-21 09:19:58.797313	ADMIN	\N
9eed4dbf-39eb-4952-98b6-18c0fc431975	niubaoxian	NIUBAOXIAN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	c1becc3b-1afc-4651-880a-4ae7c39eddce	56d01987-835f-45a5-9d29-8ced1bebe46a	\N	f	f	\N	t	0	牛保献	\N	2026-01-22 15:45:20.796202	ADMIN	\N
3758de00-e5c2-4db9-9614-9ac483aa01de	gaoxiuna	GAOXIUNA		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	51331e89-8588-402c-9603-6b86600b5fd9	d3eb8588-5e6c-4fe4-ab14-182a955f2448	\N	f	f	\N	t	0	高秀娜	\N	2026-01-22 15:45:20.796202	ADMIN	\N
71af608e-6f75-4ef3-b732-40c3abbc252a	xiaoleyuan	XIAOLEYUAN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	c58d664d-cee6-4ec3-b00f-193389489fe4	5be30d2a-e3a2-45fb-93a2-d2fac5d3c92e	\N	f	f	\N	t	0	肖乐园	\N	2026-01-22 15:45:20.796202	ADMIN	\N
11dbe01e-da63-440c-a9d4-1c4e4adb56c1	chenyan	CHENYAN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	a6be9635-3de1-4e44-b2da-a5a5200a45eb	4b60a135-4229-4678-bf71-bdbb413a1558	\N	f	f	\N	t	0	陈岩	\N	2026-01-22 15:45:20.796202	USER	\N
e9adc316-a842-4975-aec5-365e29af9755	fuyong	FUYONG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	2155c0ff-71df-4afb-9f34-85986196ca60	f521ca59-d0ab-4afd-b0dc-d30e190c98ae	\N	f	f	\N	t	0	符勇	\N	2026-01-22 15:45:20.796202	USER	\N
70adc7f1-122b-44dd-aaa1-bc7e2b10af7e	gaojianzhong	GAOJIANZHONG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	56189383-d571-41e3-bbc1-30a9033bfa15	b9afd411-31e3-48a2-8d47-d322155807a8	\N	f	f	\N	t	0	高建忠	\N	2026-01-22 15:45:20.796202	USER	\N
c315159c-03c9-4dd3-932e-5b2bef964ae3	guowen	GUOWEN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	2178c2d1-fd99-4247-b288-e826129e5989	73fe3241-8501-4f70-8c45-3e0ec8d45906	\N	f	f	\N	t	0	郭文	\N	2026-01-22 15:45:20.796202	USER	\N
6533f741-ec44-49c6-a424-1300839dc6ed	houjiaxu	HOUJIAXU		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	25943e1e-9d8d-493d-9894-d2cbabfb423a	b9972ec5-b872-45e6-88b4-78b8ac7daa12	\N	f	f	\N	t	0	侯家绪	\N	2026-01-22 15:45:20.796202	USER	\N
15a826d4-e52a-4bc7-b84b-6eaf43599eeb	hufengtao	HUFENGTAO		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	5452e538-5e15-4358-bade-ba90e1124d4b	61965d9c-056c-410f-82bb-6b9c0ecf9c5c	\N	f	f	\N	t	0	胡锋涛	\N	2026-01-22 15:45:20.796202	USER	\N
a819d51b-9e0b-4319-9d43-5d152436ea85	lishiming	LISHIMING		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	ae32c7f0-bbae-4236-92da-babec8c2d9b8	83b7e5e4-65d9-4db3-bbbe-836f4b0f7e78	\N	f	f	\N	t	0	李世铭	\N	2026-01-22 15:45:20.796202	USER	\N
1cf5202a-2693-4f1c-8ecb-4e23edfd9672	xuliang	XULIANG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	01dab920-ef48-4289-b01e-00c8dc3136f6	0f40d85a-2e27-4e22-91f1-2acd40ebd8b1	\N	f	f	\N	t	0	徐亮	\N	2026-01-22 15:45:20.796202	USER	\N
90ced372-e025-4860-80fc-bc1864999ca2	liyanjun	LIYANJUN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	6f5ffd76-43a1-415a-bbd6-98d2a3472bc0	8234fa56-aebc-4515-bc43-ce33fa49b64e	\N	f	f	\N	t	0	李艳军	\N	2026-01-22 15:45:20.796202	USER	\N
ec76c690-ef03-4383-b4e1-c2a6b8a254b5	jinfeng	JINFENG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	f9d2f808-20b7-425a-b50b-96f62d14e7c0	3da9483e-3fc4-4c54-aba0-e3a00420238f	\N	f	f	\N	t	0	靳峰	\N	2026-01-22 15:45:20.796202	USER	\N
baaf79d9-54ea-4ea7-a5d3-cbd2390fc5b5	yanning	YANNING		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	40c3b1e0-3f0f-4972-99a2-890c12d84845	c3de6ed1-f559-436b-9c18-206850f93f25	\N	f	f	\N	t	0	闫宁	\N	2026-01-22 15:45:20.796202	USER	\N
66ce7c82-30e0-4046-9ff0-9cbddbb519d5	zhoushukang	ZHOUSHUKANG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	5f0d8c44-9048-4627-8581-268b14a6edb9	d00e1625-f537-4e42-8fde-d9b1485cb34a	\N	f	f	\N	t	0	周书康	\N	2026-01-22 15:45:20.796202	USER	\N
f3bde21b-5ff9-421a-94bb-586f38c80447	weishuo	WEISHUO		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	a11778e6-0db1-4742-9648-85aa02352d00	7c8b2c9c-8a49-4857-be19-97d90c9a223a	\N	f	f	\N	t	0	魏烁	\N	2026-01-22 15:45:20.796202	USER	\N
e7817310-7e17-464c-a3c1-22a69f331d63	zhangshuhao	ZHANGSHUHAO		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	d468695b-d596-4bb3-9c58-8fdb481b8c7b	b035d57b-531d-44c3-80e2-e970b9b25a83	\N	f	f	\N	t	0	张书浩	\N	2026-01-22 15:45:20.796202	USER	\N
99c3f1fe-3b43-449c-8e83-c70b3b20ca63	wangzhiyong	WANGZHIYONG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	00dcab4c-772f-4d6a-a0ae-f8f3f8539697	ca0f2d91-4a42-4658-8829-5aef5643c386	\N	f	f	\N	t	0	王志永	\N	2026-01-22 15:45:20.796202	USER	\N
42fa0bbe-5462-406c-95e8-781bf8b0a2c6	testuser	TESTUSER		\N	t	$2a$10$1BxLsBapSIYZ9T7SJBdTVe/4j23H.0CH3XJHSE3qjL1cH9xkaeTwC	59ba27b1-71fa-49fe-8803-d47bb2af5a02	e26ffcb9-820b-42c1-99cb-efe3053d2da2	\N	f	f	\N	t	0	测试用户	测试部	2025-10-21 09:19:59.048617	USER	\N
8613aa3b-e355-4339-af9b-6a52b52409ce	madongfang	MADONGFANG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	91c1663c-ee50-4398-bbb3-49efb13fc108	91586b07-f2c6-4ae8-abab-3e128f238121	\N	f	f	\N	t	0	马东方	\N	2026-01-22 15:45:20.796202	ADMIN	\N
8780c971-1d42-4720-aadd-ffe6e69d5e0b	weiquanquan	WEIQUANQUAN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	9cf64bc0-f43a-4290-90c4-afbbb3ce1e33	f43a32ed-3580-4a8f-ba08-a8529b4d80a3	\N	f	f	\N	t	0	魏泉泉	\N	2026-01-22 15:45:20.796202	USER	\N
906383a3-805d-452e-bd17-5029ea49fa4b	jiangbao	JIANGBAO		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	a6250ed7-2b94-4dee-9733-fb8a86fe1a0c	c23867fe-061e-4389-befd-b50d08a44fbc	\N	f	f	\N	t	0	蒋豹	\N	2026-01-22 15:45:20.796202	USER	\N
620109f0-647b-47f6-92b4-1c44f2611702	songkeke	SONGKEKE		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	b1ba2631-53ef-49fd-b34d-82978cfcc4ea	137d572c-16b9-4216-a0ad-36cc5a88921c	\N	f	f	\N	t	0	宋可可	\N	2026-01-22 15:45:20.796202	USER	\N
eb4d8a48-9483-497d-8dba-1485389c4fcf	wuyingying	WUYINGYING		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	eb554264-c19a-4b5d-9a41-e2cee85c2048	6ac95c42-441d-48b9-888c-07fe2c7ef200	\N	f	f	\N	t	0	武莹莹	\N	2026-01-22 15:45:20.796202	USER	\N
e9aa2058-a8c4-4868-a688-424f5b4c4dac	jiaxinjie	JIAXINJIE		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	d1e274c3-3758-4845-97f2-816d19b40f04	76f0d340-c2cb-4d7b-8394-a45d0faba695	\N	f	f	\N	t	0	贾新杰	\N	2026-01-22 15:45:20.796202	USER	\N
6a2cb33c-8dff-45e2-befe-870c58e52869	wanghongbao	WANGHONGBAO		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	20bc507f-99d9-4a7e-a4c9-257ea10f6ee3	54929fc6-ed83-4e9d-841d-861fa078bb84	\N	f	f	\N	t	0	王红宝	\N	2026-01-22 15:45:20.796202	USER	\N
823530ce-0fa3-42c1-9101-ef8c050e0cbd	wangpengfei	WANGPENGFEI		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	12760c16-2328-448c-aea0-eef417081c12	b91b1cf0-1eee-44bd-b9be-b63b00e57aab	\N	f	f	\N	t	0	王鹏飞	\N	2026-01-22 15:45:20.796202	USER	\N
33e91754-540f-48f4-a87d-aa5a9bb44a00	sunzan	SUNZAN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	59ffc928-204b-48af-a689-22487fdeea87	8169274e-15ad-411f-826f-9deaa3e6ab99	\N	f	f	\N	t	0	孙赞	\N	2026-01-22 15:45:20.796202	USER	\N
7e3a45b3-34a1-40c6-8096-599fb2f6b76e	admin1	ADMIN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	6a331c85-9465-417f-b4f2-8f5721780da4	dadfd007-268e-4a04-bdca-4b3948ea3a27	\N	f	f	\N	t	0	系统管理员（录入）		2026-02-02 15:12:28.162491	SUB_USER	a057308f-94f4-4bc9-9bf3-6831bbc18999
854b3e84-9dd8-4393-b653-85d93094df12	niubaoxian1	NIUBAOXIAN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	a4a922a7-97bd-4679-be4b-788d2b0ea3f9	81e53dc4-68a1-4a3d-9da5-2931fe8396a7	\N	f	f	\N	t	0	牛保献（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	9eed4dbf-39eb-4952-98b6-18c0fc431975
e8bf639f-24bf-47eb-ad7d-b69ce0592ec8	gaoxiuna1	GAOXIUNA1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	d372ebb9-f6b0-4e44-8424-a8992c315e67	48f3fe58-9015-494e-aac8-db0eaeac69df	\N	f	f	\N	t	0	高秀娜（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	3758de00-e5c2-4db9-9614-9ac483aa01de
ed10951a-247b-447e-8cf3-6cba6225d70a	xiaoleyuan1	XIAOLEYUAN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	efc05278-33d9-453a-b09b-d9a011f87c74	476922d0-e971-4cdd-bceb-9bf52fefe29b	\N	f	f	\N	t	0	肖乐园（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	71af608e-6f75-4ef3-b732-40c3abbc252a
09a8b3a4-897c-471d-981f-1c55b218d25f	mazejun	MAZEJUN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	6f01458a-8ea4-4f51-bd97-6ee1c0fc1f35	a2e9b766-1ac5-41b7-b262-f7da09df8000	\N	f	f	\N	t	0	马泽军	\N	2026-01-22 15:45:20.796202	USER	\N
33048029-fd17-4662-8c6d-5c8362e2b881	zhangqingwei	ZHANGQINGWEI		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	640acada-fcbf-4ff0-8968-139029ceb084	90c57adf-4917-471f-8a77-fa80d8e00ba2	\N	f	f	\N	t	0	张庆巍	\N	2026-01-22 15:45:20.796202	USER	\N
2d90a788-e685-4c69-bf8c-180701a1d1aa	yangxirui	YANGXIRUI		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	e24aa3fa-0d2b-41fe-bfa2-b0c2b4c0d412	92f50b77-4903-4889-9bee-c19c77e421eb	\N	f	f	\N	t	0	杨希锐	\N	2026-01-22 15:45:20.796202	USER	\N
d3b6861e-c149-4804-ba2d-236cec052fd3	zhangbowei	ZHANGBOWEI		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	f5160a0d-3b92-444a-8869-e8e0506c6075	92dabcb0-a34d-41f4-880e-26c3259a9a5c	\N	f	f	\N	t	0	张博炜	\N	2026-01-22 15:45:20.796202	USER	\N
de6ae249-80e6-4e45-b03d-9dd011e54332	wangzhiming	WANGZHIMING		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	4f4aba28-2241-4582-b4ae-6710205b6362	a01911db-b850-4a01-94d0-2d54eff26059	\N	f	f	\N	t	0	王志明	\N	2026-01-22 15:45:20.796202	USER	\N
2280c6d5-89b8-4dcc-8439-6a682c0b5ba3	wangjiapeng	WANGJIAPENG		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	226f0cd6-94ee-4bc4-a585-dfe8345b896e	2a8453c1-621b-4c99-aecc-6d0ef1478823	\N	f	f	\N	t	0	王佳朋	\N	2026-01-22 15:45:20.796202	USER	\N
11b5ab36-ea7a-4fd9-8109-12b70401b187	lushen	LUSHEN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	0d321152-5098-4d27-a4bd-e6216e13defe	605b2078-1d27-48ca-b268-5d5def3cae37	\N	f	f	\N	t	0	卢申	\N	2026-01-22 15:45:20.796202	USER	\N
0fb15133-d725-448e-9b6e-4fda038e740d	zhupeiying	ZHUPEIYING		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	166e9f65-1810-4db6-8c9a-c34ef8558ad6	a01136c0-ae73-4abe-94d5-1cc6c486cb68	\N	f	f	\N	t	0	朱培营	\N	2026-01-22 15:45:20.796202	USER	\N
6f5917e0-ae86-40c2-b191-b270f2020916	lishitao	LISHITAO		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	068ed916-14b2-4891-b474-c8b2070033d5	5e68ef83-91e4-4976-a188-dd65514f0182	\N	f	f	\N	t	0	李世涛	\N	2026-01-22 15:45:20.796202	ADMIN	\N
0bf03f31-6611-476b-8dc3-32505d473537	chenlijun	CHENLIJUN		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	09f7ae4f-a352-48ba-90bd-ab76ac210956	58e27d19-0e02-4d8e-aabf-12395303d20d	\N	f	f	\N	t	0	陈莉君	\N	2026-01-22 15:45:20.796202	USER	\N
27727757-eda3-44bf-aead-5b942800e2e2	baipenghui	BAIPENGHUI		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	486126d8-5791-44e1-89dc-28135931ca8d	2a63cef0-a6d7-4dff-85ae-189c8a42a59f	\N	f	f	\N	t	0	白鹏辉	\N	2026-01-22 15:45:20.796202	USER	\N
8330a86d-2240-4997-85b2-0ef7578b3834	wanglingjie	WANGLINGJIE		\N	t	$2a$10$4De1cSUl.tqTrOkOl1VbyeSpSOZU/OaYguSCI4r/votIj/vn/C0yO	0e815770-c350-452e-9ab9-e450a80c7dad	f51117cf-5b15-4c22-8a95-d5660068b3d9	\N	f	f	\N	t	0	王凌颉	\N	2026-01-22 15:45:20.796202	USER	\N
90a84f21-20be-4728-810a-e70713e8e49e	fuyong1	FUYONG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	c5177ec1-09a8-4471-b796-5dfca62b3d67	aee15d14-4fd3-4301-af89-cdb78b2107ab	\N	f	f	\N	t	0	符勇（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	e9adc316-a842-4975-aec5-365e29af9755
72195a24-798e-411a-89bf-12ef95e758c1	gaojianzhong1	GAOJIANZHONG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	03629456-321f-4141-be74-e9c72bfc5d2a	9d486c9e-4fcc-47d4-ab54-50a4b9b51a41	\N	f	f	\N	t	0	高建忠（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	70adc7f1-122b-44dd-aaa1-bc7e2b10af7e
195c869b-2402-4f1e-a5e9-8ef1313ba716	guowen1	GUOWEN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	03acc6a0-fa35-4d1d-8cb6-9f2588c456e8	f4924c4b-11af-4efe-adb3-8932c030c9bd	\N	f	f	\N	t	0	郭文（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	c315159c-03c9-4dd3-932e-5b2bef964ae3
73d50af2-bbff-45ff-9ed7-0310cad215f2	houjiaxu1	HOUJIAXU1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	11b7655d-ee08-482c-92d9-a023d15e6de9	77ffa1ad-9f3c-4e92-a5d2-9a4073544001	\N	f	f	\N	t	0	侯家绪（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	6533f741-ec44-49c6-a424-1300839dc6ed
93a2f40b-7d86-4f50-8c60-d6f5c46b7a0d	hufengtao1	HUFENGTAO1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	88751db5-c5b9-47ea-9893-d029ce4fbb2d	76abe3a0-4271-4d2b-bbec-bb57df92d9a6	\N	f	f	\N	t	0	胡锋涛（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	15a826d4-e52a-4bc7-b84b-6eaf43599eeb
5bcea334-10f9-42bc-a9a7-fa5540695d05	lishiming1	LISHIMING1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	f1fb546b-4b14-4bea-8bc7-0d49a37dfa96	cb2f7d16-c361-4deb-a4a5-9ef103fa1373	\N	f	f	\N	t	0	李世铭（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	a819d51b-9e0b-4319-9d43-5d152436ea85
bb617218-e7a4-474b-b110-0d4134332635	xuliang1	XULIANG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	22f14eeb-c9a4-45db-a86f-53512ec59069	edeeb825-a3c1-42b5-b110-599aa54ef54a	\N	f	f	\N	t	0	徐亮（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	1cf5202a-2693-4f1c-8ecb-4e23edfd9672
c2178815-18dc-4715-a938-84b85c6bd975	liyanjun1	LIYANJUN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	8c3c14a5-68e7-4a2a-82ea-690df0ec5eca	1206745c-2f27-4636-b7a9-36a113fb42fa	\N	f	f	\N	t	0	李艳军（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	90ced372-e025-4860-80fc-bc1864999ca2
7cd684b8-ed21-4b1f-b336-7c2410dd9a84	jinfeng1	JINFENG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	ee20497a-3408-4e9c-8dd1-a579fc0c1e69	0303c8b3-0b52-43bb-89ba-e28b96e59aaf	\N	f	f	\N	t	0	靳峰（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	ec76c690-ef03-4383-b4e1-c2a6b8a254b5
45fa4b3f-8e02-4386-ad10-88c0b680b296	yanning1	YANNING1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	eeecf90a-8709-4a24-982a-f92071768955	762ba25a-4a52-4dba-a599-7a3d138ec6ac	\N	f	f	\N	t	0	闫宁（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	baaf79d9-54ea-4ea7-a5d3-cbd2390fc5b5
fb078857-b5ab-4710-9b0a-3201ad29f9c5	zhoushukang1	ZHOUSHUKANG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	ebdef16e-1ee9-48ef-b9ca-737c4c81acba	5a6917e3-b1b5-4e69-9d9b-219ba2c1e51b	\N	f	f	\N	t	0	周书康（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	66ce7c82-30e0-4046-9ff0-9cbddbb519d5
6aa545a6-9fdd-4dfc-a893-356f1802aa39	weishuo1	WEISHUO1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	27691343-f847-4762-840a-393da4a5c4cc	b0e7e17b-9f65-438c-b0a9-a6ca279fc3db	\N	f	f	\N	t	0	魏烁（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	f3bde21b-5ff9-421a-94bb-586f38c80447
4278f865-baa9-4305-b0e1-c07a834bc3c3	zhangshuhao1	ZHANGSHUHAO1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	91a22b69-0b63-4fc9-8f9e-0e6a1d0ebfcb	bd905d5c-c22c-427b-bcd4-b4b86b46b375	\N	f	f	\N	t	0	张书浩（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	e7817310-7e17-464c-a3c1-22a69f331d63
5ca747f5-2c29-4445-9ab7-5e75c19dbaf8	wangzhiyong1	WANGZHIYONG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	4cedd80e-8e19-49b8-8621-bd50c42b183f	1e4760d0-8d18-4b70-8296-bd98127db1f5	\N	f	f	\N	t	0	王志永（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	99c3f1fe-3b43-449c-8e83-c70b3b20ca63
2dabf953-134c-49c3-95ef-25c4e6861bf4	testuser21	TESTUSER21	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	eab9a402-a8d1-49b2-a0a7-af1b81403984	b66bb16f-2203-4874-8394-423a331ba73a	\N	f	f	\N	t	0	Test User 2（录入）	Test Dept	2026-02-02 15:12:28.162491	SUB_USER	336f09a6-42b7-4216-8de1-df281a9b6421
275e0d2b-3891-4e27-aa7b-08d5a8f7f13f	testuser1	TESTUSER1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	9b9ca121-f55b-4231-9404-22d5bbb10984	896cd409-3f31-448b-9a30-3c03505e169d	\N	f	f	\N	t	0	测试用户（录入）	测试部	2026-02-02 15:12:28.162491	SUB_USER	42fa0bbe-5462-406c-95e8-781bf8b0a2c6
fa966259-1588-45e9-9e7f-3ed443601f96	madongfang1	MADONGFANG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	ea126bf8-6897-4ed8-a0a8-2743804a180d	d262d88d-614f-40b5-b2c8-d84954e11148	\N	f	f	\N	t	0	马东方（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	8613aa3b-e355-4339-af9b-6a52b52409ce
88ce15be-e623-4cde-b8c2-ea4f341bdeb1	weiquanquan1	WEIQUANQUAN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	8a75cd78-a886-4f71-9d49-6d4252eaa034	c7594d02-8892-4d24-8e40-3a8601c2e6ed	\N	f	f	\N	t	0	魏泉泉（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	8780c971-1d42-4720-aadd-ffe6e69d5e0b
76c26387-5bbc-4e04-a977-909f36351f5b	jiangbao1	JIANGBAO1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	11e21f32-97d9-45d7-a0cb-edae0c190e8e	2b7aa7fa-3c02-47e0-9978-8e23aca82dda	\N	f	f	\N	t	0	蒋豹（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	906383a3-805d-452e-bd17-5029ea49fa4b
37a38a88-322d-4882-9e1d-7c36cba3413c	songkeke1	SONGKEKE1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	64fe699f-4cdb-44e0-aab0-e557aa8230e2	226fba4e-8fd3-47f8-add6-84071e4f10e0	\N	f	f	\N	t	0	宋可可（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	620109f0-647b-47f6-92b4-1c44f2611702
ca7fe14a-3da4-45b6-815c-52ce4ea8a183	wuyingying1	WUYINGYING1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	e9effbb7-b76d-4abd-a0a9-ff2a8675e6d4	755cd257-0878-4e07-b03c-a5ef1ff6db85	\N	f	f	\N	t	0	武莹莹（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	eb4d8a48-9483-497d-8dba-1485389c4fcf
582786be-be29-45d9-a3ac-8a4eff816f6d	jiaxinjie1	JIAXINJIE1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	b99d360f-1d95-4a3f-b8de-93286984e95a	f4f28981-160b-42c0-ab30-b644f2ec502a	\N	f	f	\N	t	0	贾新杰（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	e9aa2058-a8c4-4868-a688-424f5b4c4dac
fc6b7290-95dc-48fe-9907-ef8f4283133f	wanghongbao1	WANGHONGBAO1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	331e9568-900f-4899-afad-f8b38022f16f	fcae9801-84dd-4874-bb84-1b6e70a4cf49	\N	f	f	\N	t	0	王红宝（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	6a2cb33c-8dff-45e2-befe-870c58e52869
1807a484-3398-46b9-bfaf-7fb9088ac764	wangpengfei1	WANGPENGFEI1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	1a7ad520-39f1-4394-8d8f-d475955f8f96	c5a62735-d6fd-46f4-a197-36b69bf7b4fc	\N	f	f	\N	t	0	王鹏飞（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	823530ce-0fa3-42c1-9101-ef8c050e0cbd
9f86590f-6e4a-487a-9f6c-c9c6d1bb530e	chenyan1	CHENYAN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	88ab07bf-3247-45a6-9ce4-fd20ab37bbd9	7dbc9ead-6583-4c54-a8c1-55083a7f6fa6	\N	f	f	\N	t	0	陈岩（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	11dbe01e-da63-440c-a9d4-1c4e4adb56c1
93793298-6d56-4070-8b3a-2d7cbbea2d39	sunzan1	SUNZAN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	632b889d-6bbf-427b-b346-9bdd62ddc956	ad24a764-7dda-450a-87e2-2ac2ee27fdb1	\N	f	f	\N	t	0	孙赞（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	33e91754-540f-48f4-a87d-aa5a9bb44a00
56340b44-bf32-4121-b491-a8b7a7285163	mazejun1	MAZEJUN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	b6e3a645-ed59-49ee-946b-138699a943dd	c7737a24-8a19-44b4-9884-7781ccc8a4cb	\N	f	f	\N	t	0	马泽军（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	09a8b3a4-897c-471d-981f-1c55b218d25f
d9cb92e8-0394-483c-af61-303a22541a66	zhangqingwei1	ZHANGQINGWEI1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	3acf0407-7ef6-4944-b738-589ee8834c44	3845753c-5588-43aa-8ca3-5164bf8a3980	\N	f	f	\N	t	0	张庆巍（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	33048029-fd17-4662-8c6d-5c8362e2b881
1854abd5-1895-4d70-b08c-38a22b5ff483	yangxirui1	YANGXIRUI1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	7c357c69-e1a5-4f59-998d-b3fe91a7260c	05ff4c54-1bb4-4069-9be3-6b81845844b3	\N	f	f	\N	t	0	杨希锐（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	2d90a788-e685-4c69-bf8c-180701a1d1aa
8cbb7c97-fe4e-4545-88f7-166443ab643e	zhangbowei1	ZHANGBOWEI1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	f77dbca0-ef44-49d7-9f6d-ebceac3ef085	7cb5b956-7aec-4850-8e32-7e7e7f8482c0	\N	f	f	\N	t	0	张博炜（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	d3b6861e-c149-4804-ba2d-236cec052fd3
d452867b-965c-40be-b1ff-2bab3acc73b8	wangzhiming1	WANGZHIMING1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	51c1d986-b008-4c4e-9a77-64da2583e49d	b4005c26-7d14-47cf-b386-b3bce85856b1	\N	f	f	\N	t	0	王志明（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	de6ae249-80e6-4e45-b03d-9dd011e54332
5c87d66c-633a-4f2f-bb1f-71362ffd5450	wangjiapeng1	WANGJIAPENG1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	33327357-31ae-4992-b641-0082f71e5058	255bbd97-03b0-4326-b81c-0ef43e698fa3	\N	f	f	\N	t	0	王佳朋（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	2280c6d5-89b8-4dcc-8439-6a682c0b5ba3
79f0afa0-1558-4432-b8be-cc668616d8b3	lushen1	LUSHEN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	f1ec3286-bb14-40e4-aa68-936c42cb9971	09490a62-65fc-425e-86c6-c0995c1e1db4	\N	f	f	\N	t	0	卢申（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	11b5ab36-ea7a-4fd9-8109-12b70401b187
20ee4b1a-85b8-479c-834b-819230bf7622	zhupeiying1	ZHUPEIYING1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	4fbe0408-cabd-4d2c-a500-92a0dc2b8d69	41c75a17-231b-4c35-bb52-c1ac81d006b9	\N	f	f	\N	t	0	朱培营（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	0fb15133-d725-448e-9b6e-4fda038e740d
c4a1229e-1c75-41ca-8b0f-378d2ba7ffa5	lishitao1	LISHITAO1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	789b2d7c-0c4b-4336-9ebc-15272987344b	faa426f8-eec9-4f7a-bf96-395da1c62a80	\N	f	f	\N	t	0	李世涛（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	6f5917e0-ae86-40c2-b191-b270f2020916
5af3555a-e8f9-4526-89b9-db4edec52a68	chenlijun1	CHENLIJUN1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	4b748062-d43b-4420-82cd-aa9344de104f	febfc145-e818-41af-a213-9915e6c11c02	\N	f	f	\N	t	0	陈莉君（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	0bf03f31-6611-476b-8dc3-32505d473537
e763d971-983a-4ad9-8f87-fc2c784fe540	baipenghui1	BAIPENGHUI1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	0af8df90-5167-442e-be6d-1d4c75f1b62d	d6e8610a-4827-4a17-a861-7ad4624d5b32	\N	f	f	\N	t	0	白鹏辉（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	27727757-eda3-44bf-aead-5b942800e2e2
a5f93a9a-05a2-465f-8227-d8bc17c920c6	wanglingjie1	WANGLINGJIE1	\N	\N	f	$2a$10$zg0FwGlDXgnIxotnZndIDOkIdI1XOvQeEOXd4yMPTipyjMynXjKEO	668bb441-1565-4cd1-ad82-4a07f5c207f9	0e885234-0935-4b59-808a-25619c268890	\N	f	f	\N	t	0	王凌颉（录入）	\N	2026-02-02 15:12:28.162491	SUB_USER	8330a86d-2240-4997-85b2-0ef7578b3834
\.


--
-- Name: approval_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.approval_log_id_seq', 55, true);


--
-- Name: experiment_types_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.experiment_types_id_seq', 41, true);


--
-- Name: image_attachments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.image_attachments_id_seq', 36, true);


--
-- Name: images_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.images_id_seq', 22, true);


--
-- Name: instruments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.instruments_id_seq', 165, true);


--
-- Name: power_plants_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.power_plants_id_seq', 36, true);


--
-- Name: project_components_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.project_components_id_seq', 16, true);


--
-- Name: project_instruments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.project_instruments_id_seq', 10, true);


--
-- Name: projects_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.projects_id_seq', 20, true);


--
-- Name: report_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.report_items_id_seq', 319, true);


--
-- Name: reports_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.reports_id_seq', 127, true);


--
-- Name: unit_components_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.unit_components_id_seq', 5, true);


--
-- Name: units_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.units_id_seq', 187, true);


--
-- Name: approval_log approval_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.approval_log
    ADD CONSTRAINT approval_log_pkey PRIMARY KEY (id);


--
-- Name: experiment_types experiment_types_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.experiment_types
    ADD CONSTRAINT experiment_types_pkey PRIMARY KEY (id);


--
-- Name: image_attachments image_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.image_attachments
    ADD CONSTRAINT image_attachments_pkey PRIMARY KEY (id);


--
-- Name: images images_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT images_pkey PRIMARY KEY (id);


--
-- Name: instruments instruments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.instruments
    ADD CONSTRAINT instruments_pkey PRIMARY KEY (id);


--
-- Name: power_plants power_plants_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.power_plants
    ADD CONSTRAINT power_plants_name_key UNIQUE (name);


--
-- Name: power_plants power_plants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.power_plants
    ADD CONSTRAINT power_plants_pkey PRIMARY KEY (id);


--
-- Name: project_components project_components_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.project_components
    ADD CONSTRAINT project_components_pkey PRIMARY KEY (id);


--
-- Name: project_instruments project_instruments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.project_instruments
    ADD CONSTRAINT project_instruments_pkey PRIMARY KEY (id);


--
-- Name: project_instruments project_instruments_project_id_instrument_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.project_instruments
    ADD CONSTRAINT project_instruments_project_id_instrument_name_key UNIQUE (project_id, instrument_name);


--
-- Name: projects projects_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);


--
-- Name: report_items report_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_items
    ADD CONSTRAINT report_items_pkey PRIMARY KEY (id);


--
-- Name: reports reports_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT reports_pkey PRIMARY KEY (id);


--
-- Name: unit_components unit_components_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.unit_components
    ADD CONSTRAINT unit_components_pkey PRIMARY KEY (id);


--
-- Name: unit_components unit_components_unit_id_component_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.unit_components
    ADD CONSTRAINT unit_components_unit_id_component_name_key UNIQUE (unit_id, component_name);


--
-- Name: units units_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.units
    ADD CONSTRAINT units_pkey PRIMARY KEY (id);


--
-- Name: units units_power_plant_id_unit_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.units
    ADD CONSTRAINT units_power_plant_id_unit_name_key UNIQUE (power_plant_id, unit_name);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_approval_log_project_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_approval_log_project_id ON public.approval_log USING btree (project_id);


--
-- Name: idx_image_attachments_report_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_image_attachments_report_id ON public.image_attachments USING btree (report_id);


--
-- Name: idx_images_report_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_images_report_id ON public.images USING btree (report_id);


--
-- Name: idx_images_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_images_user_id ON public.images USING btree (user_id);


--
-- Name: idx_instruments_model; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_instruments_model ON public.instruments USING btree (instrument_model);


--
-- Name: idx_instruments_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_instruments_name ON public.instruments USING btree (instrument_name);


--
-- Name: idx_instruments_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_instruments_number ON public.instruments USING btree (instrument_number);


--
-- Name: idx_project_components_project_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_project_components_project_id ON public.project_components USING btree (project_id);


--
-- Name: idx_project_instruments_project_experiment; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_project_instruments_project_experiment ON public.project_instruments USING btree (project_id, experiment_type_code) WHERE (is_default = true);


--
-- Name: idx_projects_power_plant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_projects_power_plant_id ON public.projects USING btree (power_plant_id);


--
-- Name: idx_projects_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_projects_status ON public.projects USING btree (status);


--
-- Name: idx_projects_unit_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_projects_unit_id ON public.projects USING btree (unit_id);


--
-- Name: idx_projects_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_projects_user_id ON public.projects USING btree (user_id);


--
-- Name: idx_report_items_report_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_report_items_report_id ON public.report_items USING btree (report_id);


--
-- Name: idx_reports_experiment_type_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_reports_experiment_type_id ON public.reports USING btree (experiment_type_id);


--
-- Name: idx_reports_project_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_reports_project_id ON public.reports USING btree (project_id);


--
-- Name: idx_reports_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_reports_status ON public.reports USING btree (status);


--
-- Name: idx_reports_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_reports_user_id ON public.reports USING btree (user_id);


--
-- Name: idx_unit_components_unit_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_unit_components_unit_id ON public.unit_components USING btree (unit_id);


--
-- Name: idx_units_power_plant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_units_power_plant_id ON public.units USING btree (power_plant_id);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: image_attachments fk_image_attachments_report; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.image_attachments
    ADD CONSTRAINT fk_image_attachments_report FOREIGN KEY (report_id) REFERENCES public.reports(id) ON DELETE CASCADE;


--
-- Name: images fk_images_report; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT fk_images_report FOREIGN KEY (report_id) REFERENCES public.reports(id) ON DELETE CASCADE;


--
-- Name: images fk_images_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT fk_images_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: project_components fk_project_components_project; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.project_components
    ADD CONSTRAINT fk_project_components_project FOREIGN KEY (project_id) REFERENCES public.projects(id) ON DELETE CASCADE;


--
-- Name: projects fk_projects_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: report_items fk_report_items_experiment_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_items
    ADD CONSTRAINT fk_report_items_experiment_type FOREIGN KEY (experiment_type_id) REFERENCES public.experiment_types(id) ON DELETE RESTRICT;


--
-- Name: report_items fk_report_items_report; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.report_items
    ADD CONSTRAINT fk_report_items_report FOREIGN KEY (report_id) REFERENCES public.reports(id) ON DELETE CASCADE;


--
-- Name: reports fk_reports_experiment_type; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT fk_reports_experiment_type FOREIGN KEY (experiment_type_id) REFERENCES public.experiment_types(id) ON DELETE RESTRICT;


--
-- Name: reports fk_reports_instrument; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT fk_reports_instrument FOREIGN KEY (project_instrument_id) REFERENCES public.project_instruments(id) ON DELETE SET NULL;


--
-- Name: reports fk_reports_project; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT fk_reports_project FOREIGN KEY (project_id) REFERENCES public.projects(id) ON DELETE CASCADE;


--
-- Name: reports fk_reports_project_component; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT fk_reports_project_component FOREIGN KEY (project_component_id) REFERENCES public.project_components(id) ON DELETE SET NULL;


--
-- Name: reports fk_reports_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reports
    ADD CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: project_instruments project_instruments_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.project_instruments
    ADD CONSTRAINT project_instruments_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.projects(id) ON DELETE CASCADE;


--
-- Name: unit_components unit_components_unit_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.unit_components
    ADD CONSTRAINT unit_components_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES public.units(id) ON DELETE CASCADE;


--
-- Name: units units_power_plant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.units
    ADD CONSTRAINT units_power_plant_id_fkey FOREIGN KEY (power_plant_id) REFERENCES public.power_plants(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict hyD0zWx5tScPbDf4SP521k0MtNQSJKtDYoH2seUPD34e2f7nbCRG6tGA9TjnARa

