package mt4.license.com.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AccessInfo implements Serializable {
    private static final long serialVersionUID = -4863638632240421691L;

    public AccessInfo(String ip, int serialNumber) {
        super();
        this.ip = ip;
        this.timestamp = new Date();
        this.serialNumber = serialNumber;
    }

    public AccessInfo() {
        super();
    }

    private String ip;
    private int serialNumber;
    private Date timestamp;
}
