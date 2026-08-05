package com.example.employeeapp.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.employeeapp.model.Employee;
import com.example.employeeapp.repository.EmployeeRepository;

@Service
public class EmployeeService { // Simple in-memory store (for demo only)
	@Autowired
	private EmployeeRepository employeeRepository;
	private final Map<String, Employee> byEmail = new ConcurrentHashMap<>();
 
	public boolean register(Employee employee) {
		Employee emp1=employeeRepository.save(employee);
		String email = emp1.getEmail().toLowerCase();
		if (byEmail.containsKey(email))
			return false;
		byEmail.put(email, emp1);

		return true;
	}

	public Employee authenticate(String email, String password) {
		
		if (email == null)
			return null;
		Employee e = byEmail.get(email.toLowerCase());
		if (e == null)
			return null;
		return e.getPassword().equals(password) ? e : null;
	}

	public Employee findByEmail(String email) {
		if (email == null)
			return null;

		return byEmail.get(email.toLowerCase());
}}
