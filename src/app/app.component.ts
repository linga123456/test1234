import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'fsidac-outsource-frontend';
}
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BorrowOptionsComponent } from './borrow-options.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { By } from '@angular/platform-browser';

describe('BorrowOptionsComponent', () => {
  let component: BorrowOptionsComponent;
  let fixture: ComponentFixture<BorrowOptionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BorrowOptionsComponent],
      imports: [ReactiveFormsModule, MatTableModule, MatCheckboxModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BorrowOptionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render table rows equal to data length', () => {
    const rows = fixture.debugElement.queryAll(By.css('mat-row'));
    expect(rows.length).toBe(component.dataSource.data.length);
  });

  it('should initialize checkboxes for all options including "Ex"', () => {
    const checkboxes = fixture.debugElement.queryAll(By.css('mat-checkbox'));
    expect(checkboxes.length).toBe(
      component.dataSource.data.length * component.borrowOptions.length
    );
  });

  it('should update the form value when a checkbox is clicked', () => {
    const firstCheckbox = fixture.debugElement.query(By.css('mat-checkbox'));
    firstCheckbox.nativeElement.click();
    fixture.detectChanges();

    const controlName = component.getControlName(1, 'Chain');
    expect(component.form.get(controlName)?.value).toBe(true);
  });

  it('should have correct borrow options', () => {
    expect(component.borrowOptions).toEqual(['Chain', 'UB', 'TA', 'Ex']);
  });

  it('should have data source items as sc, sss, and basket', () => {
    const expectedItems = ['sc', 'sss', 'basket'];
    const actualItems = component.dataSource.data.map((item) => item.name);
    expect(actualItems).toEqual(expectedItems);
  });
});

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'app-borrow-options',
  templateUrl: './borrow-options.component.html',
  styleUrls: ['./borrow-options.component.css'],
})
export class BorrowOptionsComponent implements OnInit {
  form: FormGroup;
  borrowOptions = ['Chain', 'UB', 'TA', 'Ex']; // Added "Ex" option
  displayedColumns: string[] = ['id', 'name', 'borrowOptions'];
  dataSource = new MatTableDataSource([
    { id: 1, name: 'sc' },
    { id: 2, name: 'sss' },
    { id: 3, name: 'basket' },
  ]); // Updated data source

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({});
  }

  ngOnInit(): void {
    // Initialize form controls dynamically
    this.dataSource.data.forEach((item) => {
      this.borrowOptions.forEach((option) => {
        const controlName = this.getControlName(item.id, option);
        this.form.addControl(controlName, new FormControl(false));
      });
    });
  }

  getControlName(itemId: number, option: string): string {
    return `${itemId}_${option}`;
  }
}
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormControl } from '@angular/forms';
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'app-borrow-options',
  templateUrl: './borrow-options.component.html',
  styleUrls: ['./borrow-options.component.css'],
})
export class BorrowOptionsComponent implements OnInit {
  form: FormGroup;
  borrowOptions = ['Chain', 'UB', 'TA'];
  displayedColumns: string[] = ['id', 'name', 'borrowOptions'];
  dataSource = new MatTableDataSource([
    { id: 1, name: 'Item 1' },
    { id: 2, name: 'Item 2' },
    { id: 3, name: 'Item 3' },
  ]);

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({});
  }

  ngOnInit(): void {
    // Initialize form controls dynamically
    this.dataSource.data.forEach((item) => {
      this.borrowOptions.forEach((option) => {
        const controlName = this.getControlName(item.id, option);
        this.form.addControl(controlName, new FormControl(false));
      });
    });
  }

  getControlName(itemId: number, option: string): string {
    return `${itemId}_${option}`;
  }
}
.table-container {
  margin: 16px;
}

mat-checkbox {
  margin-right: 8px;
}

