# --- !Ups

-- Prepare database for matviews

CREATE TABLE matviews (
  mv_name NAME NOT NULL PRIMARY KEY,
  v_name NAME NOT NULL,
  last_refresh TIMESTAMP WITH TIME ZONE
);

-- Create view of celeb ids with a ts_vector of concatenated category values
CREATE VIEW celebrity_categories_v AS
SELECT c.id, to_tsvector(c.publicname || ' ' || c.roledescription || ' ' || COALESCE(string_agg(cv.publicname, ' '), ' '))
FROM celebrity c
LEFT JOIN celebritycategoryvalue ccv 
ON c.id = ccv.celebrityid
LEFT JOIN categoryvalue cv
ON cv.id = ccv.categoryvalueid
GROUP BY c.id;


