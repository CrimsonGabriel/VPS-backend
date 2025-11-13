package com.bazunia.vps.dto;
import com.bazunia.vps.model.SensorReading;
import java.util.List;
public record DataRequest(String password, List<SensorReading> sensors) {}