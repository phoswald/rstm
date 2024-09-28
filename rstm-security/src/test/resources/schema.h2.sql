CREATE TABLE IF NOT EXISTS user_ (
  username_ VARCHAR(255) PRIMARY KEY NOT NULL,
  password_ VARCHAR(255),
  roles_    VARCHAR(255)
);

MERGE INTO user_ (username_, password_, roles_) 
  VALUES ('username1', '$2a$10$mOJfZGme6tolM2tjFtb.CeMvmFKX.tuwwCb05vDm1N9N144r.PrLe', 'role1,role3');
