package mt4.license.com.service.imp;

import java.util.HashSet;
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
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean add(License license) {
        ValueOperations<String, Object> vo = redisTemplate.opsForValue();
        if (redisTemplate.hasKey(license.getKey())) {
            return false;
        }
        vo.set(license.getKey(), license);
        return true;
    }

    @Override
    public boolean get(License license) {
        ValueOperations<String, Object> vo = redisTemplate.opsForValue();
        License l = (License) vo.get(license.getKey());
        if (l != null) {
            license.setComment(l.getComment());
            license.setCompany(l.getCompany());
            license.setExpirationDate(l.getExpirationDate());
            license.setEnable(l.isEnable());
            return true;
        }
        return false;
    }

    @Override
    public boolean update(License license) {
        ValueOperations<String, Object> vo = redisTemplate.opsForValue();
        if (redisTemplate.hasKey(license.getKey())) {
            vo.set(license.getKey(), license);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(License license) {
        ValueOperations<String, Object> vo = redisTemplate.opsForValue();
        return vo.getOperations().delete(license.getKey());
    }

    @Override
    public Set<License> searchByKey(String key) {
        Set<String> keys = redisTemplate.keys("*" + key + "*");
        Set<License> set = new HashSet<>();
        for (String k : keys) {
            License license = new License(k);
            get(license);
            set.add(license);
        }
        return set;
    }

    @Override
    public Set<License> searchByCompany(String name) {
        Set<License> set = listAll();
        Set<License> result = new HashSet<>();
        for (License l : set) {
            if (l.getCompany().indexOf(name) != -1) {
                result.add(l);
            }
        }
        return result;
    }

    @Override
    public Set<License> listAll() {
        return search("*");
    }

    private Set<License> search(String string) {
        Set<String> keys = redisTemplate.keys(string);
        Set<License> set = new HashSet<>();
        for (String k : keys) {
            License license = new License(k);
            get(license);
            set.add(license);
        }
        return set;
    }

    @Override
    public boolean update(AccessInfo accessInfo) {
        String key = accessInfo.getKey() + "_AI";
        ValueOperations<String, Object> vo = redisTemplate.opsForValue();
        vo.set(key, accessInfo);
        return true;
    }

    @Override
    public boolean get(AccessInfo accessInfo) {
        ValueOperations<String, Object> vo = redisTemplate.opsForValue();
        AccessInfo ai = (AccessInfo) vo.get(accessInfo.getKey() + "_AI");
        if (ai != null) {
            accessInfo.setKey(ai.getKey());
            accessInfo.setIpList(ai.getIpList());
            return true;
        }
        return false;
    }

}