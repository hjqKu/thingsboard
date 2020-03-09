--测试学生
create table if NOT EXISTS student(
id VARCHAR(32),
name varchar(20),
sex VARCHAR(10),
grade VARCHAR(20)
);
--测试班级
create table if NOT EXISTS banji(
id VARCHAR(32),
name VARCHAR(20),
tag varchar(100)
)
--测试学生班级关系
--测试班级
create table if NOT EXISTS ban_stu(
id VARCHAR(32),
bId VARCHAR(32),
sId varchar(32)
)