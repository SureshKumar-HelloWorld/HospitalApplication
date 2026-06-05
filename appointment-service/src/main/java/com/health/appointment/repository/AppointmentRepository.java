package com.health.appointment.repository;

import com.health.appointment.entity.Appointment;
import com.health.appointment.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("select a from Appointment a where a.patientId = :patientId order by a.appointmentDate desc, a.appointmentTime desc")
    List<Appointment> findByPatientId(@Param("patientId") Long patientId);

    @Query("select a from Appointment a where a.doctor.id = :doctorId order by a.appointmentDate desc, a.appointmentTime desc")
    List<Appointment> findByDoctorId(@Param("doctorId") Long doctorId);

    @Query("select count(a) from Appointment a where a.doctor.id = :doctorId and a.appointmentDate = :date and a.appointmentTime = :time and a.status = :status")
    long countByDoctorAndDateTimeAndStatus(@Param("doctorId") Long doctorId, @Param("date") LocalDate date, @Param("time") LocalTime time, @Param("status") AppointmentStatus status);

    Page<Appointment> findAll(Pageable pageable);
}
