select 
t1.s_id as id
, t1.s_user_id as user_id
, nvl(t1.s_title,'') as title
, t1.s_desc as s_desc
, t1.s_domain_id as domain_id
, t1.s_domain as domain
, t1.S_BASE_GROUP_ID as base_group_id
, t1.s_seo_title as seo_title
, t1.s_seo_desc as seo_desc
, t1.s_seo_image_cover as seo_image_cover
, nvl(t2.s_uri,'') as uri
, nvl(t1.s_tags,'') as tags
, t1.S_CATEGORY_IDS as categories
, DECODE(t1.n_private, 1 , 'true', 'false') as private
, DECODE(t1.n_auto_relaunch, 1 , 'true', 'false') as relaunch
, t1.d_create as created_date
, t1.d_start as start_time
, t1.d_end as end_time
, t1.S_FB_PIXEL as fb_pixel
, t1.S_GG_PIXEL as gg_pixel
, t1.s_state as state
, t1.S_DESIGN_FRONT_URL as DESIGN_FRONT_URL
, t1.S_DESIGN_BACK_URL as DESIGN_BACK_URL
, DECODE(t1.N_BACK_VIEW, 1 , 'true', 'false') as back_view
, nvl(t1.S_SALE_PRICE,'0') as price
, t3.store_ids
-- , to_number(to_char(nvl(t1.d_update, t1.d_create),'yyyyMMddHH24miss')) as row_update_time
 , t1.modified_at
from tb_campaign t1
left join tb_url t2 on t2.s_domain_id = t1.s_domain_id and t2.s_ref = t1.s_id and t2.s_type = 'campaign'
left join (
    select tb_store_camp.s_campaign_id, listagg(tb_store_camp.s_id,',') within group( order by tb_store_camp.N_POSITION ) as store_ids
    from tb_store_camp group by tb_store_camp.s_campaign_id
) t3 on t3.s_campaign_id = t1.s_id
where 
pick_launching(t1.s_state) = 'launching'
and t1.modified_at > :sql_last_value 
and t1.modified_at < SYS_EXTRACT_UTC(SYSTIMESTAMP)
ORDER BY t1.modified_at ASC