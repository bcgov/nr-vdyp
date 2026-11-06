# VDYP Glossary

This glossary was assembled by scanning the repository: code comments, enum definitions, database table comments, API annotations and file formats. It is a starting point and is expected to be edited by the team.

The **Source** column shows where each definition came from:

- **Code**: taken from a comment, enum or class in the named location.
- **Schema**: taken from a Liquibase table or column comment.
- **Inferred**: written from how the term is used in code, or from general forestry usage. These entries should be confirmed by a subject matter expert.

Entries marked **TODO** have no definition in the repository.

## 1. Forestry and model terms

| Term | Definition | Source |
| --- | --- | --- |
| VDYP | Variable Density Yield Projection. Provides yield predictions for unmanaged B.C. stands in inventory and timber supply applications. | Code (`README.md`) |
| VDYP7 | The previous implementation of VDYP (Fortran and C). The Java code is a port of it, and comments refer to its routines and common blocks (for example `FIP_P/POLYDESC`). | Inferred |
| VDYP8 | The Java implementation in this repository. Also the only `calculation_engine_code` value. | Schema |
| Stand | An area of forest that is treated as a unit for projection. In VDYP a stand is represented by a polygon. | Inferred |
| Polygon | The unit of projection: a mapped area of forest with its own inventory attributes and one or more layers. Identified by `PolygonIdentifier`; in input files by `FEATURE_ID`, `MAP_ID` and `POLYGON_NUMBER`. | Code (`VdypPolygon`), Inferred |
| Layer | A storey of trees within a polygon. VDYP types are Primary (P), Secondary (S) and Veteran (V). VDYP7 layer codes also include Regeneration (Y), Residual (R) and Dead (D). | Code (`LayerType`, `Vdyp7LayerTypeCode`) |
| Primary layer | The main layer of a polygon and the one projected by default. | Code (`LayerType`, `ProjectionTypeCode`) |
| Veteran layer | Older, larger trees remaining above the main canopy, projected separately from the primary layer. | Inferred |
| Species | A tree species group within a layer, with its percentage of the layer. | Code (`VdypSpecies`) |
| Genus, SP0 | The species group code used by the model (for example a two letter genus code). `BaseVdypSpecies.genus` maps to VDYP7 `SP0V`. | Code (`BaseVdypSpecies`) |
| SP64 | The detailed species code (up to 64 species) supplied in inventory data. A species group (SP0) has an SP64 distribution (`Sp64DistributionSet`). | Code (`Sp64Distribution`), Inferred |
| Site | The site attributes of a species within a layer: site species, site curve, site index, height, total age, years to breast height and years at breast height. | Code (`BaseVdypSite`) |
| Site index (SI) | The expected height of a tree at a reference age (breast height age 50 in B.C. practice). Used to describe site productivity. | Inferred |
| Site curve | The height growth curve used to relate height, age and site index for a species. Identified by a site curve number. | Code (`BaseVdypSite.siteCurveNumber`) |
| SINDEX | The site index function library ported in `vdyp-sindex` (entry point `Sindxdll`). | Code |
| SI32 | A layer over SINDEX providing site index services used by VDYP (`vdyp-si32`). | Code |
| Total age | Age of the stand or species from germination. | Code (`BaseVdypSite.ageTotal`) |
| Years to breast height (YTBH) | Years a tree takes to grow from germination to breast height (1.3 m). | Code (`BaseVdypSite.yearsToBreastHeight`) |
| Breast height age | Age measured from when the tree reached breast height. | Code (`BaseVdypSite.yearsAtBreastHeight`) |
| Dominant height | The height of the dominant trees of a species or layer (`height` on a site). | Inferred |
| Lorey height | Basal area weighted mean height of the trees in a layer or species. | Inferred |
| Basal area (BA) | The cross-sectional area of tree stems at breast height per hectare (m2/ha). | Inferred |
| Trees per hectare (TPH) | Number of stems per hectare. Input column `STEMS_PER_HA_75`. | Inferred |
| Quadratic mean diameter (DQ) | Diameter of the tree of average basal area. | Inferred |
| Crown closure | Percentage of ground covered by tree crowns. Input column `CROWN_CLOSURE`. | Inferred |
| Percent available | Percentage of the polygon area available for forest growth (`PCTFLAND` in VDYP7). | Code (`BaseVdypPolygon.percentAvailable`) |
| Utilization class | Diameter class used to report results: `SMALL` (under 7.5 cm), `ALL` (7.5 cm and over), 7.5 to 12.5 cm, 12.5 to 17.5 cm, 17.5 to 22.5 cm, and over 22.5 cm. | Code (`UtilizationClass`) |
| Whole stem volume | Volume of the entire stem. | Code (`VolumeVariable.WHOLE_STEM_VOL`) |
| Close utilization volume | Merchantable volume to utilization limits. | Code (`VolumeVariable.CLOSE_UTIL_VOL`) |
| Net of decay | Close utilization volume less decay. | Code (`VolumeVariable`) |
| Net of decay and waste | Close utilization volume less decay and waste. | Code (`VolumeVariable`) |
| Net of decay, waste and breakage | Close utilization volume less decay, waste and breakage. Species carry volume, decay and breakage group indexes. | Code (`VolumeVariable`, `VdypSpecies`) |
| MAI | Mean annual increment: volume divided by age. Optional report column (`reportIncludeVolumeMAI`). | Inferred |
| Culmination | The age at which mean annual increment reaches its maximum. Optional report output (`reportIncludeCulminationValues`). | Inferred |
| MoF volumes and biomass | Volumes and biomass computed with B.C. Ministry of Forests methods. Optional outputs. | Code (`Parameters`), Inferred |
| CFS biomass | Biomass computed with Canadian Forest Service conversion factors. Optional output. | Code (`Parameters`) |
| CFS eco zone | Canadian Forest Service ecological zone (for example Boreal Shield, Taiga Plains). Input column `CFS_ECOZONE`. | Code (`CfsEcoZoneCode`) |
| BEC zone | Biogeoclimatic Ecosystem Classification zone of the polygon. Input column `BEC_ZONE_CODE`. | Code (`BecDefinition`), Inferred |
| Region | Coastal (C) or Interior (I). Many coefficients differ by region. | Code (`Region`) |
| Forest inventory zone (FIZ) | Inventory zone of the polygon. | Code (`BaseVdypPolygon.forestInventoryZone`) |
| BCLCS | B.C. Land Cover Classification Scheme. Input columns `BCLCS_LEVEL1_CODE` to `BCLCS_LEVEL5_CODE`. | Inferred |
| Inventory standard | The standard used to capture the inventory: `V` (VRI), `F` (FIP), `I` (Silviculture, definition TODO), or Unknown (derived from attributes). | Code (`InventoryStandardCode`) |
| FIP | Forest Inventory Planning: the older inventory standard. Input for FIPSTART. | Code, Inferred |
| VRI | Vegetation Resources Inventory: the current inventory standard. Input for VRISTART. | Code, Inferred |
| TSA | Timber Supply Area. Input columns `TSA_NAME`, `TSA_NUMBER`. | Inferred |
| TFL | Tree Farm Licence. Input column `TFL_NAME`. | Inferred |
| Stockability | Proportion of the area capable of supporting trees. Input columns `PRE_DISTURBANCE_STOCKABILITY`, `LAYER_STOCKABILITY`. | Inferred |
| Yield factor | Adjustment factor applied to projected yields. Input column `YIELD_FACTOR`. | Inferred |
| Reference year | The year the inventory attributes refer to. Input column `REFERENCE_YEAR`. | Inferred |
| Silvicultural base | Classification of disturbances in history records: natural or man made, insect attack, site preparation, planting, stand tending, unknown. | Code (`SilviculturalBaseCode`) |
| Compatibility variables | Adjustments that make projected values consistent with the supplied inventory values. Modes: none, all but volume, all. | Code (`CompatibilityVariableMode`), Inferred |

## 2. Projection process terms

| Term | Definition | Source |
| --- | --- | --- |
| Projection | A collection of all the inputs used to create a VDYP projection: a polygon file set, a layer file set, and JSON encoded parameters. Also the act of running the model. | Schema (`projection`) |
| Projection stage | One step of a projection: Initial, Adjust, Forward or Back. | Code (`ProjectionStageCode`) |
| FIPSTART | The start model for FIP inventory data. Produces VDYP polygons. Module `vdyp-fip`. | Code (`GrowthModelCode`) |
| VRISTART | The start model for VRI inventory data. Produces VDYP polygons. Module `vdyp-vri`. | Code (`GrowthModelCode`) |
| VRIADJST | VDYP7 adjustment step; a control file of this name is present in test resources. TODO: confirm whether it is ported. | Code (test resources) |
| Forward | Projection of a polygon forwards in time from its reference year. Module `vdyp-forward`. | Code |
| Back | Projection of a polygon backwards in time. Module `vdyp-back`. | Code |
| Processing mode | How a start model processes a stand (for example `FIP_Default`, `FIP_FipStart`, `FIP_DoNotProcess`). | Code (`ProcessingModeCode`) |
| Polygon mode | Per polygon processing mode: Don't Process, Start, Young, BATN, BATC. TODO: define Young, BATN and BATC. | Code (`PolygonMode`) |
| Polygon processing state | Lifecycle of a polygon inside the extended core: being defined, defined, initially processed, projected. | Code (`PolygonProcessingStateCode`) |
| Control file (`.CTR`) | Text file that configures a stage: which coefficient files to load and which input and output files to use (for example `VDYP.CTR`, `FIPSTART.CTR`, `VRISTART.CTR`). | Code (`controlmap`, test resources) |
| Control map | The parsed contents of a control file, including loaded coefficient tables. | Code (`ResolvedControlMap`) |
| Coefficient file | Data file of model coefficients loaded through the control file (for example `Becdef.dat`, `SP0DEF_v0.dat`). | Code (`io/parse/coe`) |
| Execution folder | Temporary folder in which a projection runs, created from `ExecutionFolderTemplate.zip`. Deletion can be delayed with `doDelayExecutionFolderDeletion`. | Code (`vdyp-extended-core`, `Parameters`) |
| Yield table | Tabular projection output by age or year. Formats: `TextReport`, `YieldTable`, `CSVYieldTable`, `DCSV`, `PLOTSY`. | Code (`Parameters.OutputFormat`) |
| Projection parameters | User options for a run (output format, age and year ranges, included columns, logging). Model `Parameters`; stored as JSON on the projection. | Code, Schema |
| Model parameters | Additional parameters stored as `model_parameters_json` on the projection. TODO: describe their purpose. | Schema |
| Parameter preset | A named saved copy of projection parameters. | Schema (`projection_param_preset`) |
| Debug mode | A projection that writes detailed debug logs. Allowed only for files with few enough polygons (`is_debug_mode_permitted`). | Schema (`file_mapping`) |

## 3. Input and output formats

| Term | Definition | Source |
| --- | --- | --- |
| HCSV | The CSV input format used by the API, with a polygon file and a layer file joined by `FEATURE_ID`. Projection kind `HCSV`. TODO: confirm the expansion of the abbreviation. | Code (`ProjectionRequestKind`, `HcsvPolygonRecordBean`) |
| ICSV | A projection request kind with its own polygon stream (`IcsvPolygonStream`). TODO: define. | Code |
| SCSV | A projection request kind; the endpoint is defined but not implemented. TODO: define. | Code |
| DCSV | A projection request kind and an output format; the input endpoint is not implemented. TODO: define. | Code |
| Polygon file | The polygon level input file. | Code |
| Layer file | The layer level input file. | Code |
| Result | The output of a projection, packaged as a ZIP of yield tables, reports and logs. | Code |

## 4. Application terms

| Term | Definition | Source |
| --- | --- | --- |
| VDYP user | A profile for a user that accesses VDYP; controls access to projection files, parameters and results. | Schema (`vdyp_user`) |
| User type | Access level of a user profile: Administrator, Power User, User, System. | Schema (`user_type_code`) |
| Identity provider | The service used to authenticate a user: IDIR, BCeID, BC Services Card. | Schema (`identity_provider_code`) |
| IDIR | B.C. Government employee identity. | Inferred |
| BCeID | B.C. Government business and individual identity for non-employees. | Inferred |
| BC Services Card (BCSC) | B.C. Government digital identity for residents. | Inferred |
| File set | A typed grouping of files that can be used in a projection: polygon, layer or result files. | Schema (`projection_file_set`) |
| File mapping | A mapping of an object in COMS to a file set in VDYP. | Schema (`file_mapping`) |
| Projection status | Current state of a projection: Draft, Queued, Running, Ready, Failed, Stuck, Admin Cancelled. See the lifecycle in the [architecture overview](../architecture/overview.md#6-projection-lifecycle). | Schema, Code |
| Stuck | A running projection that has not reported progress within the configured time. | Code (`ProjectionStuckDetectionJob`) |
| Admin Cancelled | A projection cancelled by an administrator, with a reason (`admin_cancel_reason`). | Schema, Code |
| Prioritized | A batch job that an administrator has moved ahead of others (`is_prioritized`). | Schema |
| Batch failure type | Phase in which a batch job failed: Processing Inputs, Projecting Stands, Aggregating Results. | Schema (`batch_failure_type_code`) |
| Projection batch mapping | A mapping of a projection to its batch job, with details that only the batch system updates. | Schema |
| Batch job | A Spring Batch job execution that runs a persistent projection. | Code |
| Partition | A slice of the input polygons processed in parallel within a batch job. | Code (`BatchInputPartitioner`) |
| Worker, replica | A running instance of the batch service, registered in `batch_worker_registry`. | Schema |
| Job claim, lease | A record that gives one replica the right to process a job until `lease_expiry_time`; renewed by heartbeat and protected by a `lease_token`. | Schema (`batch_job_claim`) |
| Heartbeat | Periodic update from a replica that renews its leases and its registry entry. | Schema, Code |
| Recovery | Taking over a job whose lease expired because its replica stopped. | Code (`BatchRecoveryMetadataService`) |
| Thread capacity | Total processing threads offered by registered replicas. | Code (`/thread-capacity`) |
| Storage cleanup | Administrator operation that deletes batch PVC job folders and records the run. | Schema (`storage_cleanup_run`) |
| Orphan file set | Files in object storage with no reference in the database, typically after a database restore. | Code (`backend/ORPHAN-FILE-CLEANUP.md`) |

## 5. Platform terms

| Term | Definition | Source |
| --- | --- | --- |
| COMS | BC Gov Common Object Management Service. Manages objects and buckets in S3 compatible storage and issues presigned URLs. | Code, Inferred |
| NATS JetStream | Messaging system used for the batch request work queue (`VDYP_BATCH_REQUESTS`). | Code (`nats/`) |
| API Gateway | BC Gov API gateway (Kong) that exposes the HCSV API to external consumers using API keys. | Code (`charts/api-gateway`) |
| Keycloak, SSO | BC Gov single sign-on service (`loginproxy.gov.bc.ca`, realm `standard`). | Code (`application.properties`) |
| OpenShift | BC Gov container platform where VDYP is deployed (namespaces `c50504-dev`, `-test`, `-prod`). | Code (`charts/api-gateway`) |
| Crunchy Postgres | PostgreSQL operator used in OpenShift. | Code (`charts/crunchy-postgres`) |
| PVC | Persistent volume claim. Used for batch working folders and access logs. | Code (`charts/app`) |
| Maintaining Vendor | The party responsible for operating and reviewing logging and exports. | Code (`log-exporter/README.md`) |
