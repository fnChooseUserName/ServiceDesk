import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from './auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  email = '';
  password = '';
  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  constructor(readonly auth: AuthService) {}

  ngOnInit(): void {
    void this.auth.restoreSession();
  }

  async login(): Promise<void> {
    this.errorMessage.set(null);
    this.submitting.set(true);
    try {
      await this.auth.login(this.email, this.password);
      this.password = '';
    } catch {
      this.errorMessage.set('Invalid email or password.');
    } finally {
      this.submitting.set(false);
    }
  }

  logout(): void {
    this.auth.logout();
  }
}
