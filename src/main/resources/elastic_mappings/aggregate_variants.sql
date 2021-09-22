select 
tbp.s_name as s_product_name
,tbp.s_campaign_id 
, tbp.n_back_view , tbp.N_DEFAULT, tbp.S_CURRENCY
,tbv.s_id, tbv.s_product_id, tbv.s_base_id, tbv.s_name, tbv.s_color_id, tbv.s_color_value, tbv.s_front_design_id, tbv.s_back_design_id
, tbv.s_front_img_url, tbv.s_back_img_url
, tbc.s_name as s_color_name,
nvl((
    select 
    JSON_ARRAYAGG(JSON_OBJECT(
        KEY 'size_id' VALUE s_size_id
        ,KEY 'size_name' VALUE tb_base_size.s_name
        , KEY 'price' VALUE s_sale_price
        , KEY 'base_id' VALUE tbp.s_base_id
        )
    ) 
    from tb_product_price 
    join tb_base_size on tb_base_size.s_id = tb_product_price.s_size_id
    where tb_product_price.s_product_id = tbv.s_product_id and PICK_APPROVED(tb_product_price.s_state) = 'approved'
),'[]') as prices
,
nvl((
    select JSON_ARRAYAGG(JSON_OBJECT(
        KEY 'id' VALUE tb_product_variant_mockup.s_id
        ,KEY 'variant_id' VALUE tb_product_variant_mockup.s_variant_id
        , KEY 'type' VALUE tb_product_variant_mockup.s_type
        , KEY 'image' VALUE tb_product_variant_mockup.s_image_url
        )
    ) 
    from tb_product_variant_mockup where tb_product_variant_mockup.s_variant_id = tbv.s_id
),'[]') as mockups
, to_number(to_char(nvl(tbv.d_update, tbv.d_create),'yyyyMMddHH24miss')) as row_update_time
from tb_product_variant tbv
join tb_base_color tbc on tbv.s_color_id = tbc.s_id
join tb_product tbp on tbp.s_id = tbv.s_product_id
where 
pick_approved(tbv.s_state) = 'approved' and pick_approved(tbp.s_state) = 'approved'
and to_number(to_char(nvl(tbv.d_update, tbv.d_create),'yyyyMMddHH24miss')) > :sql_last_value 
and to_number(to_char(nvl(tbv.d_update, tbv.d_create),'yyyyMMddHH24miss')) < to_number(to_char(sysdate,'yyyyMMddHH24miss'))
ORDER BY row_update_time ASC
