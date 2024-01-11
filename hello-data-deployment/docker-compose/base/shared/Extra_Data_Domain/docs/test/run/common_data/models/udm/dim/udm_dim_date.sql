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


  
    

  create  table "postgres"."common_data_udm"."udm_dim_date__dbt_tmp"
  as (
    select 		bhd.date_pk,
            bsd.date,
            bsd.yyyymmdd,
            bsd.yyyyq,
            bsd.yyyymm,
            bsd.yyyyww,
            bsd.epoch,
            bsd.year,
            bsd.quarter,
            bsd.month,
            bsd.week,
            bsd.day_of_year,
            bsd.day_of_quarter,
            bsd.day_of_month,
            bsd.day_of_week,
            bsd.day_of_week_iso,
            bsd.day_is_weekend,
            bsd.month_name_en,
            bsd.month_name_short3_en,
            bsd.day_name_en,
            bsd.day_name_short3_en,
            bsd.day_name_short2_en,
            bsd.day_suffix_en,
            bsd.month_name_de,
            bsd.month_name_short3_de,
            bsd.day_name_de,
            bsd.day_name_short3_de,
            bsd.day_name_short2_de,
            bsd.first_day_of_week,
            bsd.last_day_of_week,
            bsd.first_day_of_month,
            bsd.last_day_of_month,
            bsd.first_day_of_quarter,
            bsd.last_day_of_quarter,
            bsd.first_day_of_year,
            bsd.last_day_of_year,
            bsd.n_days_in_month,
            bsd.year_is_leap_year
from        "postgres"."common_data_bdv"."bdv_hub_date" bhd
join		"postgres"."common_data_bdv"."bdv_sat_date" bsd
on			bhd.date_pk = bsd.date_pk
order by	bhd.yyyymmdd
  );
  