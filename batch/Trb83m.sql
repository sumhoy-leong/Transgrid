username;ellipsero
pwd;ell83devro
url;jdbc:oracle:thin:@vsvaix61xx0010:1521:elldev
schema;
driver;oracle.jdbc.OracleDriver
howmanytable;2
Table1;MSF200~select * from ellipse.msf200 where rownum < 200
Table2;MSF20A~select * from ellipse.msf20A where supplier_no in (select supplier_no from ellipse.msf200 where rownum < 200)