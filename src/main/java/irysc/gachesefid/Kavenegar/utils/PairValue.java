/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irysc.gachesefid.Kavenegar.utils;

/**
 *
 * @author Hadi
 */
public class PairValue {

    private Object Key;
    private Object Value;

    public PairValue(Object Key, Object Value)
    {
        this.Key = Key;
        this.Value = Value;
    }

    public Object getKey() {
        return Key;
    }

    public Object getValue() {
        return Value;
    }
    
}
