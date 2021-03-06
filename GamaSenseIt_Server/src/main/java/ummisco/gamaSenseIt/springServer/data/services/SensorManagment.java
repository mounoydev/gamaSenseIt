package ummisco.gamaSenseIt.springServer.data.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import ummisco.gamaSenseIt.springServer.data.model.Sensor;
import ummisco.gamaSenseIt.springServer.data.model.SensorData;
import ummisco.gamaSenseIt.springServer.data.model.DisplayedData;
import ummisco.gamaSenseIt.springServer.data.model.ParameterMetadata;
import ummisco.gamaSenseIt.springServer.data.model.ParameterMetadata.DataFormat;
import ummisco.gamaSenseIt.springServer.data.model.ParameterMetadata.DataParameter;
import ummisco.gamaSenseIt.springServer.data.model.SensorMetadata;
import ummisco.gamaSenseIt.springServer.data.model.SensoredBulkData;
import ummisco.gamaSenseIt.springServer.data.repositories.ISensorDataRepository;
import ummisco.gamaSenseIt.springServer.data.repositories.IParameterMetadataRepository;
import ummisco.gamaSenseIt.springServer.data.repositories.ISensorRepository;
import ummisco.gamaSenseIt.springServer.data.repositories.ISensorMetadataRepository;
import ummisco.gamaSenseIt.springServer.data.repositories.ISensoredBulkDataRepository;

@Service("SensorManagment")
public class SensorManagment implements ISensorManagment{

	@Autowired
	IParameterMetadataRepository parameterSensorRepo;
	@Autowired
	ISensorRepository sensorRepo;
	@Autowired
	ISensoredBulkDataRepository bulkDataRepo;
	@Autowired
	ISensorMetadataRepository sensorMetadataRepo;
	@Autowired
	ISensorDataRepository analysedDataRepo;
	@Autowired
	ISensorDataAnalyser dataAnalyser;
	
	@Override
	public void saveDefaultSensorInit() {
		
		GeometryFactory gf=new GeometryFactory();
		
		SensorMetadata mtype = new SensorMetadata( DEFAULT_SENSOR_TYPE_NAME,DEFAULT_SENSOR_VERSION);
		sensorMetadataRepo.save(mtype);
		
		Point p = gf.createPoint(new Coordinate(12.3, 5.2));
		Sensor s1 = new Sensor(DEFAULT_SENSOR_NAME,p,mtype);
		sensorRepo.save(s1);
		
		
		SensorMetadata smd = new SensorMetadata("capMetadata", "v0", ":");
		addSensorMetadata(smd);
		ParameterMetadata p1 = new ParameterMetadata("temperature","c",DataFormat.DOUBLE,DataParameter.TEMPERATURE);
		ParameterMetadata p2 = new ParameterMetadata("humidity","c",DataFormat.STRING,DataParameter.TEMPERATURE);
		addParameterToSensorMetadata(smd, p1);
		addParameterToSensorMetadata(smd, p2);
		
		Sensor sx = new Sensor("node_1",p,smd);
		sensorRepo.save(sx);

	}
	

	@Override
	public void saveData(String message, Date date) {
		
		String[] data = message.split(";");
		
	 	long capturedateS=Long.valueOf(data[0]).longValue();
	 	String sensorName=data[1];
	 	long token=Long.valueOf(data[2]).longValue();
	 	String contents=data[3];
	 	List<Sensor> foundSensors = sensorRepo.findByName(sensorName);
	 	Sensor selectedSensor = null;
	 	if(foundSensors.isEmpty()) {
			SensorMetadata typeSens = sensorMetadataRepo.findByNameAndVersion(DEFAULT_SENSOR_TYPE_NAME,DEFAULT_SENSOR_VERSION).get(0);
	 		GeometryFactory gf=new GeometryFactory();
			Point p = gf.createPoint(new Coordinate(0, 0));
			selectedSensor = new Sensor(sensorName,p,typeSens);
			sensorRepo.save(selectedSensor);
	 	}
	 	else
	 	{
	 		selectedSensor = foundSensors.get(0);
	 	}
		
	 	Date capturedate = new Date(capturedateS*1000);
	 	SensoredBulkData bulkData = new SensoredBulkData(selectedSensor,token,capturedate,date,contents);
	 	bulkDataRepo.save(bulkData);
	 	List<SensorData> aData = dataAnalyser.analyseBulkData(contents, capturedate, selectedSensor);
	 	analysedDataRepo.saveAll(aData);
	}


	@Override
	public Sensor updateSensorInformation(Sensor s) {
		return sensorRepo.save(s);
	}

	@Override
	public SensorMetadata addSensorMetadata(SensorMetadata s) {
		sensorMetadataRepo.save(s);
		return s;
	}
	

	@Override
	public ParameterMetadata addParameterToSensorMetadata(SensorMetadata s, ParameterMetadata md) {
		md.setSensorMetadata(s);
		ParameterMetadata res = parameterSensorRepo.save(md);
		s.addmeasuredData(md);
		sensorMetadataRepo.save(s);
		return res;
	}
	
	
	

}
