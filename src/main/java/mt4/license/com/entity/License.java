package mt4.license.com.entity;

import java.io.Serializable;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.ToString;
import mt4.license.com.util.Utils;

@Data
@ToString
public class License implements Serializable {

    public License() {
        super();
        key = Utils.getSalt();
        expirationDate = new Date();
        enable = true;
    }

    public License(String key) {
        super();
        this.key = key;
    }

    private String key;

    private String company;

    private String comment;

    private boolean enable;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expirationDate;
}
