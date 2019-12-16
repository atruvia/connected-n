import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Game } from './Game';
import { HttpClient } from '@angular/common/http';
import { toArray } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GameService {

  private BASE_URL ='/api/games';

  constructor( private http: HttpClient) { }

  getAll(): Observable<Game[]> {
    return this.http.get<Game>(
      this.BASE_URL,
      {
        headers: {
          'Content-Type': 'application/json'
        }
      })
      .pipe( toArray() )

  }
}
