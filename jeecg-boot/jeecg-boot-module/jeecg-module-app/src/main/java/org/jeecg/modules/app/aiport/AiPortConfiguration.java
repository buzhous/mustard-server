package org.jeecg.modules.app.aiport;

import org.jeecg.modules.ai.port.AiAuthPort;
import org.jeecg.modules.ai.port.ItemReadPort;
import org.jeecg.modules.ai.port.ItemWritePort;
import org.jeecg.modules.ai.port.TaxonomyPort;
import org.jeecg.modules.app.service.AppOpenService;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.IUserCategoryService;
import org.jeecg.modules.app.service.IUserTagService;
import org.jeecg.modules.app.service.IUserVirtualStationService;
import org.jeecg.modules.app.service.IUserStationBindService;
import org.jeecg.modules.app.service.IWarehouseStationService;
import org.jeecg.modules.app.service.ITopicService;
import org.jeecg.modules.app.service.impl.OpenAppCredentialServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ai 模块端口桥接配置：向 ai 模块注册 app 侧端口实现
 */
@Configuration
public class AiPortConfiguration {

    private final AppOpenService appOpenService;
    private final IItemInfoService itemInfoService;
    private final IItemSyncService itemSyncService;
    private final OpenAppCredentialServiceImpl credentialService;
    private final IUserCategoryService userCategoryService;
    private final IUserTagService userTagService;
    private final ITopicService topicService;
    private final IUserVirtualStationService virtualStationService;
    private final IUserStationBindService stationBindService;
    private final IWarehouseStationService warehouseStationService;

    public AiPortConfiguration(AppOpenService appOpenService,
                               IItemInfoService itemInfoService,
                               IItemSyncService itemSyncService,
                               OpenAppCredentialServiceImpl credentialService,
                               IUserCategoryService userCategoryService,
                               IUserTagService userTagService,
                               ITopicService topicService,
                               IUserVirtualStationService virtualStationService,
                               IUserStationBindService stationBindService,
                               IWarehouseStationService warehouseStationService) {
        this.appOpenService = appOpenService;
        this.itemInfoService = itemInfoService;
        this.itemSyncService = itemSyncService;
        this.credentialService = credentialService;
        this.userCategoryService = userCategoryService;
        this.userTagService = userTagService;
        this.topicService = topicService;
        this.virtualStationService = virtualStationService;
        this.stationBindService = stationBindService;
        this.warehouseStationService = warehouseStationService;
    }

    @Bean
    public AiAuthPort aiAuthPort() {
        return new AiAuthPortImpl();
    }

    @Bean
    public TaxonomyPort aiTaxonomyPort() {
        return new AiTaxonomyPortImpl(userCategoryService, userTagService, topicService,
                virtualStationService, stationBindService, warehouseStationService);
    }

    @Bean
    public AiItemPortImpl aiItemPort() {
        // AiItemPortImpl 同时实现 ItemWritePort 与 ItemReadPort，按接口类型注入均可解析
        return new AiItemPortImpl(appOpenService, itemInfoService, itemSyncService, credentialService);
    }
}
