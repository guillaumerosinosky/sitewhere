/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.rdb.repositories;

import com.sitewhere.rdb.entities.AssetType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public interface AssetTypeRepository extends CrudRepository<AssetType, UUID>, PagingAndSortingRepository<AssetType, UUID>, JpaSpecificationExecutor<AssetType> {

    Optional<AssetType> findByToken(String token);
}
