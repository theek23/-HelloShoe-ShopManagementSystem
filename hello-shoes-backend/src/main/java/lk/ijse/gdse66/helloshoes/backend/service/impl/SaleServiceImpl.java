package lk.ijse.gdse66.helloshoes.backend.service.impl;

import lk.ijse.gdse66.helloshoes.backend.dto.*;
import lk.ijse.gdse66.helloshoes.backend.dto.basic.InventoryBasicDTO;
import lk.ijse.gdse66.helloshoes.backend.dto.basic.SaleBasicDTO;
import lk.ijse.gdse66.helloshoes.backend.entity.*;
import lk.ijse.gdse66.helloshoes.backend.repo.*;
import lk.ijse.gdse66.helloshoes.backend.service.SaleService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author: Theekshana De Silva,
 * @Runtime version: 11.0.11+9-b1341.60amd64
 **/
@Service
@Transactional
public class SaleServiceImpl implements SaleService {
    private SaleRepo saleRepo;
    private SaleDetailRepo saleDetailRepo;
    private InventoryRepo inventoryRepo;
    private CustomerRepo customerRepo;
    private EmployeeRepo employeeRepo;

    private ModelMapper modelMapper;

    public SaleServiceImpl(SaleRepo saleRepo, SaleDetailRepo saleDetailRepo, InventoryRepo inventoryRepo, CustomerRepo customerRepo, EmployeeRepo employeeRepo, ModelMapper modelMapper) {
        this.saleRepo = saleRepo;
        this.saleDetailRepo = saleDetailRepo;
        this.inventoryRepo = inventoryRepo;
        this.customerRepo = customerRepo;
        this.employeeRepo = employeeRepo;
        this.modelMapper = modelMapper;
    }

    @Override
    public String generateNewID() {
        String lastID = saleRepo.findLastOrderCode();

        if (lastID == null){
            return "O00001";
        }
        String numericPart = lastID.substring(5);
        int numericValue = Integer.parseInt(numericPart);

        // Increment the numeric value
        numericValue++;

        // Format the new ID with leading zeros
        String newID = String.format("O%05d", numericValue);

        return newID;
    }

    @Override
    public List<SaleBasicDTO> getAllOrdersFormLast3Days() {
        return saleRepo.findAllOrdersFromLast3Days().stream().map(
                sale -> modelMapper.map(sale, SaleBasicDTO.class)).toList();
    }

    @Override
    public long contActiveSales() {
        return saleRepo.countActiveSales();
    }

    @Override
    public double findTotalProfitOfSoldItems() {
        return saleDetailRepo.findTotalProfitOfSoldItems();
    }

    @Override
    public double findTotalInventoryCost() {
        return inventoryRepo.findTotalInventoryCost();
    }

    @Override
    public List<InventoryBasicDTO> getTop4SoldItems() {
        List<InventoryBasicDTO> items = new ArrayList<>();
        List<Object[]> results = saleDetailRepo.findTop4SoldItems();

        for (Object[] result : results) {
            String itemCode = (String) result[0];
            BigDecimal totalQtyDecimal = (BigDecimal) result[1];
            Integer totalQty = totalQtyDecimal.intValueExact();


            Inventory inventory = inventoryRepo.findById(itemCode).orElse(null);
            if (inventory != null) {
                InventoryBasicDTO inventoryDTO = modelMapper.map(inventory, InventoryBasicDTO.class);
                inventoryDTO.setPicture(Base64.getEncoder().encodeToString(inventory.getPicture()));
                // You can add totalQty to the InventoryDTO if needed
                 inventoryDTO.setQty(totalQty); // Ensure you have a setter for this in InventoryDTO
                items.add(inventoryDTO);
            }
        }

        return items;
    }

    @Override
    public SaleDTO placeSale(SaleDTO saleDTO) {
        Employee employee;
        Customer customer = customerRepo.findById(saleDTO.getCustomer().getCustomerCode())
                .orElseThrow(() -> new NoSuchElementException("Customer not found with code: " + saleDTO.getCustomer().getCustomerCode()));

        if (saleDTO.getEmployee().getEmployeeCode() != null) {
            employee = employeeRepo.findById(saleDTO.getEmployee().getEmployeeCode())
                    .orElseThrow(() -> new NoSuchElementException("Employee not found with code: " + saleDTO.getEmployee().getEmployeeCode()));
        } else {
            employee = employeeRepo.findById("EMP00001")
                    .orElseThrow(() -> new NoSuchElementException("Employee not found with code: " + saleDTO.getEmployee().getEmployeeCode()));
        }

        Sale sale = modelMapper.map(saleDTO, Sale.class);
        sale.setCustomer(customer);
        sale.setEmployee(employee);
        sale.setCustomerName(customer.getName());
        sale.setEmployeeName(employee.getName());

        // Update customer's recent purchase date and total points
        customer.setRecentPurchase(new Timestamp(System.currentTimeMillis()));
        customer.setTotalPoints(customer.getTotalPoints() + saleDTO.getAddedPoints());

        customerRepo.save(customer);
        saleRepo.save(sale);

        // Update inventory qty in inventory entity.
        for (SaleDetail saleDetail : sale.getSaleDetail()) {
            Inventory inventory = inventoryRepo.findById(saleDetail.getItemCode())
                    .orElseThrow(() -> new NoSuchElementException("Inventory not found with item code: " + saleDetail.getItemCode()));
            inventory.setQty(inventory.getQty() - saleDetail.getQty());
            inventoryRepo.save(inventory);
        }

        return saleDTO;
    }


    @Override
    public List<SaleBasicDTO> getAllOrders() {
        return saleRepo.findAll().stream().map(
                sale -> modelMapper.map(sale, SaleBasicDTO.class)).toList();
    }

    @Override
    public List<SaleDetailDTO> getAllOrderDetails() {
        return saleDetailRepo.findAll().stream().map(
                saleDetail -> modelMapper.map(saleDetail, SaleDetailDTO.class)).toList();
    }
    //Refund Only
    @Override
    public void updateSale(String id, SaleDTO saleDTO) {
        Sale existingSale = saleRepo.findById(id).orElseThrow(() -> new RuntimeException("Sale not found with ID: " + id));
        modelMapper.map(saleDTO, existingSale);
        existingSale.setTotal(0.00);
        existingSale.setStatus("Refunded");
        saleRepo.save(existingSale);
    }
}
