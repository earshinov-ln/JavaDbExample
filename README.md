# JavaDbExample

Демонстрация использования Java-технологий для работы с БД, пока только JDBC.

Проект представляет собой консольную программу, которая работает через JDBC с базой данных Derby.
Перед запуском программы необходимо создать базу `Lesson22` и заполнить её, как описано ниже.
Требуется запустить сетевой сервер Derby на локальном хосте по стандартному порту 1527 командой `startNetworkServer`.

SQL для заполнения базы:

    create table Employee (
      EMPNO int not null primary key,
      ENAME varchar (50) not null,
      JOB_TITLE varchar (150) not null );

    insert into Employee values
      (7369, 'John Smith', 'Clerk'),
      (7499, 'Joe  Allen', 'Salesman'),
      (7521, 'Mary Lou', 'Director');

    alter table Employee
      add column DUPLICATE_EMPNO int
      references Employee
      on delete set null;

Для запуска программы из командной строки после сборки проекта:

  * Перейти в каталог `bin/` со скомпилированными Java-классами
  * Выполнить что-то типа `java -cp .:../lib/derbyclient.jar name.earshinov.DbExample.Main АРГУМЕНТЫ`
    (для Windows, вероятно, вместо `:` и `/` надо использовать `;` и `\` соответственно).
    Аргументы для начала можно не указывать, программа выведет справку.

## Что необходимо для работы над проектом

  * Eclipse
  * Apache Derby.  Если установлен Glassfish, можно использовать версию Derby, поставляемую с ним.
    Её можно найти в подпапке `javadb/` папки, в которую установлен Glassfish.
