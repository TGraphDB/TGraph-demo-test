
CREATE TABLE cross_node (
id int PRIMARY KEY,
name char(255)
)


CREATE TABLE road (
id int PRIMARY KEY,
r_name char(16),
r_start int,
r_end int,
r_length int,
r_type int
)

CREATE TABLE temporal_status (
t int,
rid int,
status int(1),
travel_t int,
seg_cnt int
)

