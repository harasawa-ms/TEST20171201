package com.kerosoft.mands.test20171201;

/**
 * Created by taka on 2017/12/17.
 */

public class Rireki {


    public Rireki() {
        super();
    }

    public static Rireki parse(byte[] res,int off){

        Rireki self = new Rireki();
        self.init(res,off);
        return self;
    }

    private void init(byte[] res,int off){

    }

}
