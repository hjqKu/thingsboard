/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.loit.service.install.update;

import com.loit.common.data.SearchTextBased;
import com.loit.common.data.id.UUIDBased;
import com.loit.common.data.page.TextPageData;
import com.loit.common.data.page.TextPageLink;

public abstract class PaginatedUpdater<I, D extends SearchTextBased<? extends UUIDBased>> {

    private static final int DEFAULT_LIMIT = 100;

    public void updateEntities(I id) {
        TextPageLink pageLink = new TextPageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        while (hasNext) {
            TextPageData<D> entities = findEntities(id, pageLink);
            for (D entity : entities.getData()) {
                updateEntity(entity);
            }
            hasNext = entities.hasNext();
            if (hasNext) {
                pageLink = entities.getNextPageLink();
            }
        }
    }

    protected abstract TextPageData<D> findEntities(I id, TextPageLink pageLink);

    protected abstract void updateEntity(D entity);

}
