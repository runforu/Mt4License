package mt4.license.com.entity;

import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AccessInfo {
    private String key;
    private List<String> ipList;
}
