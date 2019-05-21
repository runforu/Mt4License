package mt4.license.com.service.imp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import mt4.license.com.entity.AccessInfo;
import mt4.license.com.entity.License;
import mt4.license.com.service.RedisService;

@Service
public class RedisServiceImpl implements RedisService {

    @Resource
    private RedisTemplate<String, Object> licenseTemplate;

    @Resource
    private RedisTemplate<String, AccessInfo> accessInfoTemplate;

    @Override
    public boolean addLicense(License license) {
        ValueOperations<String, Object> vo = licenseTemplate.opsForValue();
        if (licenseTemplate.hasKey(license.getKey())) {
            return false;
        }
        vo.set(license.getKey(), license);
        return true;
    }

    @Override
    public boolean getLicense(License license) {
        ValueOperations<String, Object> vo = licenseTemplate.opsForValue();
        try {
            License l = (License) vo.get(license.getKey());
            if (l != null) {
                license.setComment(l.getComment());
                license.setCompany(l.getCompany());
                license.setExpirationDate(l.getExpirationDate());
                license.setEnable(l.isEnable());
                return true;
            }
        } catch (Exception e) {
        }

        return false;
    }

    @Override
    public boolean updateLicense(License license) {
        ValueOperations<String, Object> vo = licenseTemplate.opsForValue();
        if (licenseTemplate.hasKey(license.getKey())) {
            vo.set(license.getKey(), license);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteLicense(License license) {
        ValueOperations<String, Object> vo = licenseTemplate.opsForValue();
        return vo.getOperations().delete(license.getKey());
    }

    @Override
    public Set<License> searchLicenseByKey(String key) {
        return search("*" + key + "*");
    }

    @Override
    public Set<License> searchLicenseByCompany(String name) {
        Set<License> set = listLicense();
        Set<License> result = new HashSet<>();
        for (License l : set) {
            if (l.getCompany().indexOf(name) != -1) {
                result.add(l);
            }
        }
        return result;
    }

    @Override
    public Set<License> listLicense() {
        return search("*");
    }

    private Set<License> search(String string) {
        Set<String> keys = getLicenseKeys(string);
        Set<License> set = new HashSet<>();
        for (String k : keys) {
            License license = new License(k);
            if (getLicense(license)) {
                set.add(license);
            }
        }
        return set;
    }

    @Override
    public boolean push(License license, AccessInfo ai) {
        if (license.getKey() == null) {
            return false;
        }
        licenseTemplate.opsForList().leftPush(license.getKey() + "_AI", ai);
        return true;
    }

    @Override
    public List<AccessInfo> range(License license, int start, int end) {
        if (license.getKey() == null) {
            return null;
        }
        return accessInfoTemplate.opsForList().range(license.getKey() + "_AI", start, end);
    }

    private Set<String> getLicenseKeys(String pattern) {
        Set<String> keys = licenseTemplate.keys(pattern);
        keys.removeIf((key) -> {
            return key.endsWith("_AI") || key.length() < 16;
        });
        return keys;
    }

    private Set<String> getAccessInfoKeys(String pattern) {
        Set<String> keys = accessInfoTemplate.keys(pattern);
        keys.removeIf((key) -> {
            return !key.endsWith("_AI");
        });
        return keys;
    }
}