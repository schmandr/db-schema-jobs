/*
Please don't use any schema names in the view definition; use the ${dbSchema} variable instead.
Always use CREATE OR REPLACE VIEW, not CREATE VIEW, as the script must be repeatable.
All view names must end in "_v".

Example:

CREATE OR REPLACE VIEW ${dbSchema}.t_ili2db_model_v AS
SELECT
    filename,
    iliversion,
    modelname,
    importdate
FROM
    ${dbSchema}.t_ili2db_model
*/

CREATE OR REPLACE VIEW ${dbSchema}.t_ili2db_model_v AS
SELECT
    filename,
    iliversion,
    modelname,
    importdate
FROM
    ${dbSchema}.t_ili2db_model
;
