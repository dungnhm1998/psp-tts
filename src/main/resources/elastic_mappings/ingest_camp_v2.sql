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
, t4.s_gender as s_genders
, t3.s_base_id as s_base_ids
, decode(t1.s_base_group_id, 
    'gCce1pA0CHaMfOOK', t3.S_DEFAULT_COLOR_ID
    ,'phonecase-group', t3.S_DEFAULT_COLOR_ID
    ,'45GIOu2TGamLQwFV', t3.S_DEFAULT_COLOR_ID
    ,'dtyb7T3qjOqiijCq', t3.S_DEFAULT_COLOR_ID
    ,'NVngNagqBFbd3011',t3.S_DEFAULT_COLOR_ID
    , t3.S_COLORS) as s_colors
, t3.s_sizes
, t1.MODIFIED_AT
from tb_campaign t1
left join tb_url t2 on t2.s_ref = t1.s_id and t2.s_type = 'campaign'
join (
    select tb_product.s_campaign_id, tb_product.s_base_id, tb_product.s_colors, tb_product.s_default_color_id, tb_product.s_sizes
    from tb_product where product_default(n_default) = 1 
    and tb_product.s_state = 'approved'
) t3 on t1.s_id = t3.s_campaign_id
join tb_base t4 on t3.s_base_id = t4.s_id
where
t1.s_state = 'launching'
 and MODIFIED_AT > :sql_last_value
 and MODIFIED_AT < SYS_EXTRACT_UTC(SYSTIMESTAMP)
 and (s_sub_state is null or s_sub_state = 'approved')
and t1.s_id not in (select S_ID from TB_CAMPAIGN_TAKEDOWN where s_state <> 'delete')
and t1.n_private = 0
-- and t1.s_user_id = 'A656'
-- and t1.s_id in  ('A656-15', 'A656-14')
ORDER BY t1.MODIFIED_AT ASC