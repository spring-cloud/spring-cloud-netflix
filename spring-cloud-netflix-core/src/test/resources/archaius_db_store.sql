create table if not exists properties (
  property_key VARCHAR(40) NOT NULL PRIMARY KEY,
  property_value VARCHAR(255) NOT NULL,
);

insert into properties(property_key, property_value) values ('db.property','this is a db property');
insert into properties(property_key, property_value) values ('db.second.property','this is another db property');

