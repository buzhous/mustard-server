-- 主题列表视觉补充：按主题名称补齐子标题（topic_subtitle）与背景图（bg_image）
-- 背景图为 AI 生成的统一插画风格分类图，已上传 OSS：
--   https://msdoss.buzhous.com/app/topic_bg/cat_{1..36}.jpg
-- 幂等：UPDATE 为绝对值覆盖，重复执行结果一致
UPDATE `tbl_topic` SET `topic_subtitle` = '电脑外设 · 办公设备', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_1.jpg'  WHERE `topic_code` = 'cat_1';
UPDATE `tbl_topic` SET `topic_subtitle` = '手机平板 · 智能设备', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_2.jpg'  WHERE `topic_code` = 'cat_2';
UPDATE `tbl_topic` SET `topic_subtitle` = '扳手电钻 · 维修工具', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_3.jpg'  WHERE `topic_code` = 'cat_3';
UPDATE `tbl_topic` SET `topic_subtitle` = '冰箱洗衣机 · 大小家电', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_4.jpg'  WHERE `topic_code` = 'cat_4';
UPDATE `tbl_topic` SET `topic_subtitle` = '锅碗餐具 · 家居好物', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_5.jpg'  WHERE `topic_code` = 'cat_5';
UPDATE `tbl_topic` SET `topic_subtitle` = '清洁剂 · 日常耗材', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_6.jpg'  WHERE `topic_code` = 'cat_6';
UPDATE `tbl_topic` SET `topic_subtitle` = '洗护用品 · 浴室好物', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_7.jpg'  WHERE `topic_code` = 'cat_7';
UPDATE `tbl_topic` SET `topic_subtitle` = '衣帽围巾 · 穿搭配饰', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_8.jpg'  WHERE `topic_code` = 'cat_8';
UPDATE `tbl_topic` SET `topic_subtitle` = '背包行李箱 · 收纳包袋', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_9.jpg'  WHERE `topic_code` = 'cat_9';
UPDATE `tbl_topic` SET `topic_subtitle` = '雨伞雨衣 · 钥匙门卡', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_10.jpg' WHERE `topic_code` = 'cat_10';
UPDATE `tbl_topic` SET `topic_subtitle` = '项链手镯 · 珠宝玉石', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_11.jpg' WHERE `topic_code` = 'cat_11';
UPDATE `tbl_topic` SET `topic_subtitle` = '球鞋皮鞋 · 鞋靴护理', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_12.jpg' WHERE `topic_code` = 'cat_12';
UPDATE `tbl_topic` SET `topic_subtitle` = '哑铃瑜伽垫 · 家用健身', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_13.jpg' WHERE `topic_code` = 'cat_13';
UPDATE `tbl_topic` SET `topic_subtitle` = '球拍球鞋 · 运动护具', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_14.jpg' WHERE `topic_code` = 'cat_14';
UPDATE `tbl_topic` SET `topic_subtitle` = '帐篷睡袋 · 露营装备', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_15.jpg' WHERE `topic_code` = 'cat_15';
UPDATE `tbl_topic` SET `topic_subtitle` = '行车记录仪 · 车品配件', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_16.jpg' WHERE `topic_code` = 'cat_16';
UPDATE `tbl_topic` SET `topic_subtitle` = '瓜果蔬菜 · 新鲜食材', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_17.jpg' WHERE `topic_code` = 'cat_17';
UPDATE `tbl_topic` SET `topic_subtitle` = '米面粮油 · 干货调料', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_18.jpg' WHERE `topic_code` = 'cat_18';
UPDATE `tbl_topic` SET `topic_subtitle` = '零食饮料 · 冲调咖啡', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_19.jpg' WHERE `topic_code` = 'cat_19';
UPDATE `tbl_topic` SET `topic_subtitle` = '盆栽多肉 · 鲜花绿植', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_20.jpg' WHERE `topic_code` = 'cat_20';
UPDATE `tbl_topic` SET `topic_subtitle` = '常备药品 · 保健用品', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_21.jpg' WHERE `topic_code` = 'cat_21';
UPDATE `tbl_topic` SET `topic_subtitle` = '口红精华 · 护肤彩妆', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_22.jpg' WHERE `topic_code` = 'cat_22';
UPDATE `tbl_topic` SET `topic_subtitle` = '奶粉尿裤 · 育儿好物', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_23.jpg' WHERE `topic_code` = 'cat_23';
UPDATE `tbl_topic` SET `topic_subtitle` = '猫粮狗粮 · 宠物玩具', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_24.jpg' WHERE `topic_code` = 'cat_24';
UPDATE `tbl_topic` SET `topic_subtitle` = '棋牌桌游 · 文娱设备', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_25.jpg' WHERE `topic_code` = 'cat_25';
UPDATE `tbl_topic` SET `topic_subtitle` = '手办盲盒 · 潮流玩具', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_26.jpg' WHERE `topic_code` = 'cat_26';
UPDATE `tbl_topic` SET `topic_subtitle` = '笔记本 · 文具画具', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_27.jpg' WHERE `topic_code` = 'cat_27';
UPDATE `tbl_topic` SET `topic_subtitle` = '图书画册 · 收藏典籍', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_28.jpg' WHERE `topic_code` = 'cat_28';
UPDATE `tbl_topic` SET `topic_subtitle` = '身份证件 · 合同票据', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_29.jpg' WHERE `topic_code` = 'cat_29';
UPDATE `tbl_topic` SET `topic_subtitle` = '收到的礼 · 装饰摆件', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_30.jpg' WHERE `topic_code` = 'cat_30';
UPDATE `tbl_topic` SET `topic_subtitle` = '吉他钢琴 · 乐器配件', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_31.jpg' WHERE `topic_code` = 'cat_31';
UPDATE `tbl_topic` SET `topic_subtitle` = '纪念币 · 手信藏品', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_32.jpg' WHERE `topic_code` = 'cat_32';
UPDATE `tbl_topic` SET `topic_subtitle` = '房产车辆 · 大件资产', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_33.jpg' WHERE `topic_code` = 'cat_33';
UPDATE `tbl_topic` SET `topic_subtitle` = '存单保单 · 权证合同', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_34.jpg' WHERE `topic_code` = 'cat_34';
UPDATE `tbl_topic` SET `topic_subtitle` = '账号皮肤 · 游戏装备', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_35.jpg' WHERE `topic_code` = 'cat_35';
UPDATE `tbl_topic` SET `topic_subtitle` = '域名素材 · 数字资产', `bg_image` = 'https://msdoss.buzhous.com/app/topic_bg/cat_36.jpg' WHERE `topic_code` = 'cat_36';
