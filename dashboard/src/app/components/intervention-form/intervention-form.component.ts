import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, inject, input, output, signal, viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  CAUSE_LABELS, Intervention, InterventionCause, MAX_DESCRIPTION_LENGTH, MAX_DURATION_MINUTES,
} from '../../core/intervention.model';
import { errorMessage, InterventionService } from '../../core/intervention.service';
import { MACHINE_NAMES } from '../../core/kpi.model';

/**
 * Formulaire de saisie d'une intervention (rôle OPERATEUR), dans une fenêtre modale (<dialog>).
 * Les mêmes règles sont vérifiées ici (pour aider l'utilisateur) et côté serveur (pour la sécurité).
 */
@Component({
  selector: 'fp-intervention-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  templateUrl: './intervention-form.component.html',
  styleUrl: './intervention-form.component.css',
})
export class InterventionFormComponent implements AfterViewInit {
  /** Machine et alerte pré-remplies quand on vient d'une alerte. */
  readonly machineId = input<string>('M1');
  readonly alertId = input<string | null>(null);

  readonly saved = output<Intervention>();
  readonly closed = output<void>();

  private readonly service = inject(InterventionService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  readonly machines = Object.entries(MACHINE_NAMES).map(([id, name]) => ({ id, name }));
  readonly causes = Object.entries(CAUSE_LABELS).map(([value, label]) => ({ value: value as InterventionCause, label }));
  readonly maxDescription = MAX_DESCRIPTION_LENGTH;
  readonly maxDuration = MAX_DURATION_MINUTES;

  readonly submitting = signal(false);
  readonly serverError = signal<string | null>(null);

  readonly form = inject(FormBuilder).nonNullable.group({
    machineId: ['M1', Validators.required],
    cause: ['MECANIQUE' as InterventionCause, Validators.required],
    description: ['', [Validators.required, Validators.maxLength(MAX_DESCRIPTION_LENGTH)]],
    durationMinutes: [15, [Validators.required, Validators.min(1), Validators.max(MAX_DURATION_MINUTES)]],
  });

  ngAfterViewInit(): void {
    this.form.controls.machineId.setValue(this.machineId());
    if (this.alertId()) {
      this.form.controls.machineId.disable();   // liée à une alerte : la machine est imposée
    }
    this.dialog().nativeElement.showModal();
  }

  invalid(name: 'description' | 'durationMinutes'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || control.dirty);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.serverError.set(null);
    this.service
      .create({ ...value, description: value.description.trim(), alertId: this.alertId() })
      .subscribe({
        next: (intervention) => {
          this.submitting.set(false);
          this.saved.emit(intervention);
          this.close();
        },
        error: (e: HttpErrorResponse) => {
          this.submitting.set(false);
          this.serverError.set(errorMessage(e));
        },
      });
  }

  close(): void {
    this.dialog().nativeElement.close();
    this.closed.emit();
  }
}
