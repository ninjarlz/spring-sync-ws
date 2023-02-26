create table Todo (
	id int not null AUTO_INCREMENT,
	description varchar(2000),
	complete boolean,
	primary key (id)
);

insert into Todo (description, complete) values ('A', false);
insert into Todo (description, complete) values ('B', false);
insert into Todo (description, complete) values ('C', false);
