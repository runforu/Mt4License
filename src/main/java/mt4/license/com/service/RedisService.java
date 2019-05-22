package mt4.license.com.service;

import java.util.List;
import java.util.Set;

import mt4.license.com.entity.AccessInfo;
import mt4.license.com.entity.License;

public interface RedisService {

    public boolean addLicense(License license);

    public boolean getLicense(License license);

    public boolean updateLicense(License license);

    public boolean deleteLicense(License license);

    public boolean deleteAccessInfo(License license);

    public Set<License> searchLicenseByKey(String key);

    public Set<License> listLicense();

    public Set<License> searchLicenseByCompany(String name);

    // License access history
    public boolean push(License license, AccessInfo ai);

    public List<AccessInfo> range(License license, int start, int end);

    public void trimList(License license, int capacity);

    public AccessInfo getFirst(License license);
}