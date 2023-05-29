-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database
insert into passport (id, serial, number, date) values(1, '9922', '987654', '2022-01-09');
insert into passport (id, serial, number, date) values(2, '9922', '987655', '2022-01-09');
insert into passport (id, serial, number, date) values(3, '9922', '987656', '2022-01-09');
alter sequence passport_seq restart with 4;