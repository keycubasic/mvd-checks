-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database

alter table passport
    add column textsearchable_index_col tsvector
        generated always as (to_tsvector('russian', serial || ' ' || number)) stored;

create index passport_idx on passport using gin (textsearchable_index_col);

create or replace function isPresent(
    serial_ text,
    number_ text) returns int as $$
   declare result int;
   begin
        select sign(count(1)) into result from passport
        where textsearchable_index_col @@ to_tsquery('russian', serial_ || ' & ' || number_);
        return result;
   end;
$$ language plpgsql;

-- select * from isPresent('9922', '987654');

insert into passport (id, serial, number, date) values(1, '9922', '987654', '2022-01-09');
insert into passport (id, serial, number, date) values(2, '9922', '987655', '2022-01-09');
insert into passport (id, serial, number, date) values(3, '9922', '987656', '2022-01-09');
alter sequence passport_seq restart with 4;