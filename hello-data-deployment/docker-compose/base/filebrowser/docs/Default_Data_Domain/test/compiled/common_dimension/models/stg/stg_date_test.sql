--
-- Copyright © 2024, Kanton Bern
-- All rights reserved.
--
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are met:
--     * Redistributions of source code must retain the above copyright
--       notice, this list of conditions and the following disclaimer.
--     * Redistributions in binary form must reproduce the above copyright
--       notice, this list of conditions and the following disclaimer in the
--       documentation and/or other materials provided with the distribution.
--     * Neither the name of the <organization> nor the
--       names of its contributors may be used to endorse or promote products
--       derived from this software without specific prior written permission.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
-- ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
-- WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
-- DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
-- DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
-- (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
-- LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
-- (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
-- SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--










-- Generated by dbtvault.

    

WITH source_data AS (

    SELECT

    datum_id,
    datum,
    jahr,
    quatal,
    monat,
    woche,
    tag,
    wochetag,
    jahrtag

    FROM postgres.common_dimension_stg.raw_date
),

derived_columns AS (

    SELECT

    datum_id,
    datum,
    jahr,
    quatal,
    monat,
    woche,
    tag,
    wochetag,
    jahrtag,
    'raw_date' AS record_source,
    CURRENT_TIMESTAMP AS load_date

    FROM source_data
),

hashed_columns AS (

    SELECT

    DATUM_ID,
    DATUM,
    JAHR,
    QUATAL,
    MONAT,
    WOCHE,
    TAG,
    WOCHETAG,
    JAHRTAG,
    RECORD_SOURCE,
    LOAD_DATE,

    CAST(UPPER(MD5(NULLIF(CONCAT_WS('||',
        COALESCE(NULLIF(UPPER(TRIM(CAST(datum_id AS VARCHAR))), ''), '^^')
    ), '^^'))) AS BYTEA) AS datum_pk

    FROM derived_columns
),

columns_to_select AS (

    SELECT

    DATUM_ID,
    DATUM,
    JAHR,
    QUATAL,
    MONAT,
    WOCHE,
    TAG,
    WOCHETAG,
    JAHRTAG,
    RECORD_SOURCE,
    LOAD_DATE,
    DATUM_PK

    FROM hashed_columns
)

SELECT * FROM columns_to_select