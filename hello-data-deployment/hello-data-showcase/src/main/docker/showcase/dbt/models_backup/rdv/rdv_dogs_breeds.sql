-- Dieses Modell wird nur benötigt weil dbt-utils.unpivot() als Input nur ein Modell und keine Source referenzieren kann.
select *
from {{ source('lzn', 'dogs_breeds') }}

