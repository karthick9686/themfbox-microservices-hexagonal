package com.hexagonal.portfolio.adapter.out.persistence.entity.primary;

import jakarta.persistence.*;

@Entity
@Table(name = "users_mapping")
public class UsersMapping
{
    Integer id;
    String mapping_name;
    String investor_name;
    String pan;
    String client_name;
    Integer user_id;
    Integer investor_id;
    String relation;

    @Transient
    Double aum;
    @Transient
    String mobile;
    @Transient
    String email;
    @Transient
    String rm_name;

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getInvestor_name() {
        return investor_name;
    }
    public void setInvestor_name(String investor_name) {
        this.investor_name = investor_name;
    }
    public String getPan() {
        return pan;
    }
    public void setPan(String pan) {
        this.pan = pan;
    }
    public String getMapping_name() {
        return mapping_name;
    }
    public void setMapping_name(String mapping_name) {
        this.mapping_name = mapping_name;
    }
    public String getClient_name() {
        return client_name;
    }
    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }
    public Integer getUser_id() {
        return user_id;
    }
    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }
    public Integer getInvestor_id() {
        return investor_id;
    }
    public void setInvestor_id(Integer investor_id) {
        this.investor_id = investor_id;
    }
    public String getRelation() {
        return relation;
    }
    public void setRelation(String relation) {
        this.relation = relation;
    }



    @Transient
    public String getMobile() {
        return mobile;
    }
    @Transient
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    @Transient
    public String getEmail() {
        return email;
    }
    @Transient
    public void setEmail(String email) {
        this.email = email;
    }
    @Transient
    public Double getAum() {
        return aum;
    }
    @Transient
    public void setAum(Double aum) {
        this.aum = aum;
    }
    @Transient
    public String getRm_name() {
        return rm_name;
    }
    @Transient
    public void setRm_name(String rm_name) {
        this.rm_name = rm_name;
    }

}
