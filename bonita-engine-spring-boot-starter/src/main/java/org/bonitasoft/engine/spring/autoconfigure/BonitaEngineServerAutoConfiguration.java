/**
 * Copyright (C) 2019 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.spring.autoconfigure;

import org.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.spring.autoconfigure.properties.BonitaEngineProperties;
import org.bonitasoft.engine.test.TestEngine;
import org.bonitasoft.engine.test.TestEngineImpl;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(BonitaEngineCommonAutoConfiguration.class)
public class BonitaEngineServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TestEngine bonitaEngine(BonitaEngineProperties properties) {
        TestEngine instance = TestEngineImpl.getInstance();
        instance.setBonitaDatabaseProperties(properties.getDatabase().getBonita());
        instance.setBusinessDataDatabaseProperties(properties.getDatabase().getBusinessData());
        instance.setDropOnStart(false);
        instance.setDropOnStop(false);
        return instance;
    }

    @Bean
    @ConditionalOnMissingBean
    APIClient bonitaClient() {
        return new APIClient();
    }

}