create table if not exists planet
(
    name    text,
    id      serial primary key,
    created timestamp with time zone
);