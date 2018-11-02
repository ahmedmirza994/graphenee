/*******************************************************************************
 * Copyright (c) 2016, 2018 Farrukh Ijaz
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
 *******************************************************************************/
package io.graphenee.security.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.graphenee.core.api.GxNamespaceService;
import io.graphenee.core.enums.AccessTypeStatus;
import io.graphenee.core.model.BeanCollectionFault;
import io.graphenee.core.model.BeanFault;
import io.graphenee.core.model.api.GxDataService;
import io.graphenee.core.model.bean.GxAccessKeyBean;
import io.graphenee.core.model.bean.GxNamespaceBean;
import io.graphenee.core.model.bean.GxResourceBean;
import io.graphenee.core.model.bean.GxSecurityGroupBean;
import io.graphenee.core.model.bean.GxSecurityPolicyBean;
import io.graphenee.core.model.bean.GxSecurityPolicyDocumentBean;
import io.graphenee.core.model.bean.GxUserAccountBean;
import io.graphenee.core.model.entity.GxAccessKey;
import io.graphenee.core.model.entity.GxNamespace;
import io.graphenee.core.model.entity.GxResource;
import io.graphenee.core.model.entity.GxSecurityGroup;
import io.graphenee.core.model.entity.GxSecurityPolicy;
import io.graphenee.core.model.entity.GxSecurityPolicyDocument;
import io.graphenee.core.model.entity.GxUserAccount;
import io.graphenee.core.model.jpa.repository.GxAccessKeyRepository;
import io.graphenee.core.model.jpa.repository.GxAccessLogRepository;
import io.graphenee.core.model.jpa.repository.GxNamespaceRepository;
import io.graphenee.core.model.jpa.repository.GxResourceRepository;
import io.graphenee.core.model.jpa.repository.GxSecurityGroupRepository;
import io.graphenee.core.model.jpa.repository.GxSecurityPolicyDocumentRepository;
import io.graphenee.core.model.jpa.repository.GxSecurityPolicyRepository;
import io.graphenee.core.model.jpa.repository.GxUserAccountRepository;
import io.graphenee.security.api.GxSecurityDataService;
import io.graphenee.security.exception.GxPermissionException;

@Service
@ConditionalOnProperty(prefix = "graphenee", name = "modules.enabled", havingValue = "true")
@Transactional
public class GxSecurityDataServiceImpl implements GxSecurityDataService {

	@Autowired
	GxDataService dataService;
	@Autowired
	GxAccessKeyRepository gxAccessKeyRepository;
	@Autowired
	GxResourceRepository gxResourceRepository;
	@Autowired
	GxUserAccountRepository gxUserAccountRepository;
	@Autowired
	GxSecurityGroupRepository securityGroupRepo;
	@Autowired
	GxSecurityPolicyRepository securityPolicyRepo;
	@Autowired
	GxNamespaceRepository namespaceRepo;
	@Autowired
	GxSecurityPolicyDocumentRepository securityPolicyDocumentRepo;
	@Autowired
	GxAccessLogRepository accessLogRepo;
	@Autowired
	GxResourceRepository resourceRepo;

	@Autowired
	GxNamespaceService namespaceService;

	@Override
	public void access(GxNamespaceBean gxNamespaceBean, String accessKey, String resourceName, Timestamp timeStamp) throws GxPermissionException {
		UUID accessKeyUuid = UUID.fromString(accessKey);
		GxUserAccount gxUserAccount = gxUserAccountRepository.findByGxAccessKeysKeyAndGxAccessKeysIsActiveTrue(accessKeyUuid);

		if (gxUserAccount != null) {
			if (canAccessResource(gxNamespaceBean, accessKey, resourceName, timeStamp)) {
				dataService.log(gxNamespaceBean, accessKey, resourceName, timeStamp, AccessTypeStatus.ACCESS.statusCode(), true);
			} else {
				dataService.log(gxNamespaceBean, accessKey, resourceName, timeStamp, AccessTypeStatus.ACCESS.statusCode(), false);
				throw new GxPermissionException("access permission denied.");
			}
		} else
			throw new GxPermissionException("Key not in use.");
	}

	@Override
	public void checkIn(GxNamespaceBean gxNamespaceBean, String accessKey, String resourceName, Timestamp timeStamp) throws GxPermissionException {
		UUID accessKeyUuid = UUID.fromString(accessKey);
		GxUserAccount gxUserAccount = gxUserAccountRepository.findByGxAccessKeysKeyAndGxAccessKeysIsActiveTrue(accessKeyUuid);

		if (gxUserAccount != null) {
			if (canAccessResource(gxNamespaceBean, accessKey, resourceName, timeStamp)) {
				dataService.log(gxNamespaceBean, accessKey, resourceName, timeStamp, AccessTypeStatus.CHECKIN.statusCode(), true);
			} else {
				dataService.log(gxNamespaceBean, accessKey, resourceName, timeStamp, AccessTypeStatus.CHECKIN.statusCode(), false);
				throw new GxPermissionException("check-in permission denied.");
			}
		} else
			throw new GxPermissionException("Key not in use.");
	}

	@Override
	public void checkOut(GxNamespaceBean gxNamespaceBean, String accessKey, String resourceName, Timestamp timeStamp) throws GxPermissionException {
		UUID accessKeyUuid = UUID.fromString(accessKey);
		GxUserAccount gxUserAccount = gxUserAccountRepository.findByGxAccessKeysKeyAndGxAccessKeysIsActiveTrue(accessKeyUuid);

		if (gxUserAccount != null) {
			if (canAccessResource(gxNamespaceBean, accessKey, resourceName, timeStamp)) {
				dataService.log(gxNamespaceBean, accessKey, resourceName, timeStamp, AccessTypeStatus.CHECKOUT.statusCode(), true);
			} else {
				dataService.log(gxNamespaceBean, accessKey, resourceName, timeStamp, AccessTypeStatus.CHECKOUT.statusCode(), false);
				throw new GxPermissionException("check-out permission denied.");
			}
		} else
			throw new GxPermissionException("key not in use.");
	}

	@Override
	public boolean canAccessResource(GxNamespaceBean gxNamespaceBean, String accessKey, String resourceName, Timestamp timeStamp) throws GxPermissionException {
		UUID accessKeyUuid = UUID.fromString(accessKey);
		GxAccessKeyBean accessKeyBean = makeAccessKeyBean(gxAccessKeyRepository.findByKey(accessKeyUuid));
		GxResource gxResource = gxResourceRepository.findOneByResourceNameAndGxNamespaceNamespaceAndIsActiveTrue(resourceName, gxNamespaceBean.getNamespace());
		if (gxResource == null)
			throw new GxPermissionException("rescource not found.");
		GxResourceBean resourceBean = makeResourceBean(gxResource);
		return accessKeyBean.canDoAction(resourceBean.getResourceName(), "access");
	}

	private GxNamespaceBean makeNamespaceBean(GxNamespace entity) {
		GxNamespaceBean bean = new GxNamespaceBean();
		bean.setOid(entity.getOid());
		bean.setNamespace(entity.getNamespace());
		bean.setNamespaceDescription(entity.getNamespaceDescription());
		bean.setIsActive(entity.getIsActive());
		bean.setIsProtected(entity.getIsProtected());
		return bean;
	}

	private GxSecurityPolicyBean makeSecurityPolicyBean(GxSecurityPolicy entity) {
		GxSecurityPolicyBean bean = new GxSecurityPolicyBean();
		bean.setOid(entity.getOid());
		bean.setPriority(entity.getPriority());
		bean.setSecurityPolicyName(entity.getSecurityPolicyName());
		bean.setSecurityPolicyDescription(entity.getSecurityPolicyDescription());
		bean.setIsActive(entity.getIsActive());
		bean.setIsProtected(entity.getIsProtected());
		bean.setNamespaceFault(BeanFault.beanFault(entity.getGxNamespace().getOid(), (oid) -> {
			return makeNamespaceBean(namespaceRepo.findOne(oid));
		}));
		bean.setSecurityGroupCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return securityGroupRepo.findAllByGxSecurityPoliciesOidEquals(entity.getOid()).stream().map(this::makeSecurityGroupBean).collect(Collectors.toList());
		}));
		bean.setAccessKeyCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return gxAccessKeyRepository.findAllByGxSecurityPolicysOidEquals(entity.getOid()).stream().map(this::makeAccessKeyBean).collect(Collectors.toList());
		}));
		bean.setSecurityPolicyDocumentCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return securityPolicyDocumentRepo.findAllByGxSecurityPolicyOidEquals(entity.getOid()).stream().map(this::makeSecurityPolicyDocumentBean).collect(Collectors.toList());
		}));
		return bean;
	}

	private GxSecurityPolicyDocumentBean makeSecurityPolicyDocumentBean(GxSecurityPolicyDocument entity) {
		GxSecurityPolicyDocumentBean bean = new GxSecurityPolicyDocumentBean();
		bean.setOid(entity.getOid());
		bean.setDocumentJson(entity.getDocumentJson());
		bean.setTag(entity.getTag());
		bean.setIsDefault(entity.getIsDefault());
		bean.setSecurityPolicyBeanFault(new BeanFault<Integer, GxSecurityPolicyBean>(entity.getGxSecurityPolicy().getOid(), oid -> {
			return makeSecurityPolicyBean(securityPolicyRepo.findOne(oid));
		}));
		return bean;
	}

	private GxSecurityGroupBean makeSecurityGroupBean(GxSecurityGroup entity) {
		GxSecurityGroupBean bean = new GxSecurityGroupBean();
		bean.setOid(entity.getOid());
		bean.setSecurityGroupName(entity.getSecurityGroupName());
		bean.setSecurityGroupDescription(entity.getSecurityGroupDescription());
		bean.setPriority(entity.getPriority());
		bean.setIsActive(entity.getIsActive());
		bean.setIsProtected(entity.getIsProtected());
		bean.setNamespaceFault(BeanFault.beanFault(entity.getGxNamespace().getOid(), (oid) -> {
			return makeNamespaceBean(namespaceRepo.findOne(oid));
		}));
		bean.setSecurityPolicyCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return securityPolicyRepo.findAllByGxSecurityGroupsOidEquals(entity.getOid()).stream().map(this::makeSecurityPolicyBean).collect(Collectors.toList());
		}));
		bean.setAccessKeyCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return gxAccessKeyRepository.findAllByGxSecurityPolicysOidEquals(entity.getOid()).stream().map(this::makeAccessKeyBean).collect(Collectors.toList());
		}));
		return bean;
	}

	private GxResourceBean makeResourceBean(GxResource gxResource) {
		GxResourceBean bean = new GxResourceBean();
		bean.setOid(gxResource.getOid());
		bean.setResourceName(gxResource.getResourceName());
		bean.setResourceDescription(gxResource.getResourceDescription());
		bean.setIsActive(gxResource.getIsActive());
		bean.setGxNamespaceBeanFault(BeanFault.beanFault(gxResource.getGxNamespace().getOid(), oid -> {
			return namespaceService.namespace(gxResource.getResourceName());
		}));
		return bean;
	}

	private List<GxResourceBean> makeResourceBean(List<GxResource> resources) {
		List<GxResourceBean> beans = new ArrayList<>();
		resources.forEach(resource -> {
			beans.add(makeResourceBean(resource));
		});
		return beans;
	}

	private GxUserAccountBean makeUserAccountBean(GxUserAccount entity) {
		GxUserAccountBean bean = new GxUserAccountBean();
		bean.setOid(entity.getOid());
		bean.setUsername(entity.getUsername());
		bean.setEmail(entity.getEmail());
		bean.setFirstName(entity.getFirstName());
		bean.setLastName(entity.getLastName());
		bean.setFullNameNative(entity.getFullNameNative());
		bean.setIsLocked(entity.getIsLocked());
		bean.setIsActive(entity.getIsActive());
		bean.setIsPasswordChangeRequired(entity.getIsPasswordChangeRequired());
		bean.setIsProtected(entity.getIsProtected());
		bean.setSecurityGroupCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return securityGroupRepo.findAllByGxUserAccountsOidEquals(entity.getOid()).stream().map(this::makeSecurityGroupBean).collect(Collectors.toList());
		}));
		bean.setSecurityPolicyCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return securityPolicyRepo.findAllByGxUserAccountsOidEquals(entity.getOid()).stream().map(this::makeSecurityPolicyBean).collect(Collectors.toList());
		}));
		bean.setAccessKeyCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return gxAccessKeyRepository.findAllByGxUserAccountOidEquals(entity.getOid()).stream().map(this::makeAccessKeyBean).collect(Collectors.toList());
		}));
		return bean;
	}

	private GxAccessKeyBean makeAccessKeyBean(GxAccessKey gxAccessKey) {
		GxAccessKeyBean bean = new GxAccessKeyBean();
		bean.setOid(gxAccessKey.getOid());
		bean.setKey(gxAccessKey.getKey());
		bean.setSecret(gxAccessKey.getSecret());
		bean.setIsActive(gxAccessKey.getIsActive());
		bean.setType(gxAccessKey.getType());

		bean.setSecurityGroupCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return securityGroupRepo.findAllByGxAccessKeysOidEquals(gxAccessKey.getOid()).stream().map(this::makeSecurityGroupBean).collect(Collectors.toList());
		}));
		bean.setSecurityPolicyCollectionFault(BeanCollectionFault.collectionFault(() -> {
			return securityPolicyRepo.findAllByGxAccessKeysOidEquals(gxAccessKey.getOid()).stream().map(this::makeSecurityPolicyBean).collect(Collectors.toList());
		}));
		if (gxAccessKey.getGxUserAccount() != null)
			bean.setUserAccountBeanFault(new BeanFault<Integer, GxUserAccountBean>(gxAccessKey.getGxUserAccount().getOid(), (oid) -> {
				return makeUserAccountBean(gxUserAccountRepository.findOne(oid));
			}));
		return bean;
	}

	private GxResource toEntity(GxResourceBean bean, GxResource entity) {
		entity.setResourceName(bean.getResourceName());
		entity.setResourceDescription(bean.getResourceDescription());
		entity.setIsActive(bean.getIsActive());
		if (bean.getGxNamespaceBeanFault() != null) {
			entity.setGxNamespace(namespaceRepo.findOne(bean.getGxNamespaceBeanFault().getOid()));
		}
		return entity;
	}

	@Override
	public GxResourceBean createOrUpdate(GxResourceBean bean) {
		GxResource gxResource;
		if (bean.getOid() == null)
			gxResource = new GxResource();
		else
			gxResource = gxResourceRepository.findOne(bean.getOid());
		gxResource = gxResourceRepository.save(toEntity(bean, gxResource));
		bean.setOid(gxResource.getOid());
		return bean;
	}

	@Override
	public List<GxResourceBean> findResourceByNamespace(GxNamespaceBean gxNamespaceBean) {
		return makeResourceBean(resourceRepo.findAllByGxNamespaceNamespace(gxNamespaceBean.getNamespace()));
	}

	@Override
	public void delete(GxResourceBean bean) {
		gxResourceRepository.delete(bean.getOid());
	}

}
