create table BattleYutUser(
user_num int unsigned not null auto_increment primary key,
id varchar(20) not null,
nickname varchar(20) not null,
password varchar(20) not null,
win_num int unsigned not null,
lose_num int unsigned not null
  );  

