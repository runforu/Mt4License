package mt4.license.com.entity;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotEmpty;

import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.ToString;
import mt4.license.com.util.Utils;

@Data
@ToString
public class License implements Serializable {

    private static final long serialVersionUID = -3859874746947862044L;

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

    @NotEmpty(message = "Company name cannot be empty")
    @Length(min = 6, message = "Company name must not be shorter than 6")
    @Length(max = 32, message = "Company name must not be longer than 32")
    private String company;

    private String comment;

    private boolean enable;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expirationDate;
}
