-- La entidad RegistroInformativo.contenido usa @Lob sobre un String; en
-- Hibernate 5.6 + MySQL8Dialect eso se valida contra una columna LONGTEXT
-- (java.sql.Types.CLOB -> "longtext" en el dialecto de MySQL), no contra
-- TEXT como se creo originalmente en V2__registro_informativo.sql. Sin
-- este ajuste, Hibernate falla el arranque con:
--   Schema-validation: wrong column type encountered in column [contenido]
--   in table [registro_informativo]; found [text], but expecting [longtext]
-- Ampliar TEXT (max 64KB) a LONGTEXT (max 4GB) no trunca ni pierde datos
-- existentes: es un ensanchamiento, no un cambio de tipo incompatible.
ALTER TABLE registro_informativo MODIFY COLUMN contenido LONGTEXT NOT NULL;
