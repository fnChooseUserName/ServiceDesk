import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login-page',
  imports: [FormsModule],
  templateUrl: './login-page.html',
})
export class LoginPage {
  email = '';
  password = '';
  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  async login(): Promise<void> {
    this.errorMessage.set(null);
    this.submitting.set(true);
    try {
      await this.auth.login(this.email, this.password);
      await this.router.navigate(['/tickets']);
    } catch {
      this.errorMessage.set('Invalid email or password.');
    } finally {
      this.submitting.set(false);
    }
  }
}
