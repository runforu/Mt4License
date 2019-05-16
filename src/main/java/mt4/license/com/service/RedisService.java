package mt4.license.com.service;

import java.util.Set;

import mt4.license.com.entity.AccessInfo;
import mt4.license.com.entity.License;

public interface RedisService {

    public boolean add(License license);

    public boolean get(License license);

    public boolean update(License license);

    public boolean delete(License license);

    public Set<License> searchByKey(String key);

    public Set<License> listAll();

    public Set<License> searchByCompany(String name);

    public boolean update(AccessInfo accessInfo);

    public boolean get(AccessInfo accessInfo);
}