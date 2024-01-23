///
/// Copyright © 2024, Kanton Bern
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without
/// modification, are permitted provided that the following conditions are met:
///     * Redistributions of source code must retain the above copyright
///       notice, this list of conditions and the following disclaimer.
///     * Redistributions in binary form must reproduce the above copyright
///       notice, this list of conditions and the following disclaimer in the
///       documentation and/or other materials provided with the distribution.
///     * Neither the name of the <organization> nor the
///       names of its contributors may be used to endorse or promote products
///       derived from this software without specific prior written permission.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
/// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
/// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
/// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
/// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
/// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
/// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
///

import {Component, OnDestroy, OnInit} from '@angular/core';
import {Observable, Subscription, tap} from "rxjs";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "../../../../store/app/app.state";
import {selectEditedFaq} from "../../../../store/faq/faq.selector";
import {Navigate} from "../../../../store/app/app.action";
import {Faq} from "../../../../store/faq/faq.model";
import {DeleteEditedFaq, SaveChangesToFaq, ShowDeleteFaqPopup} from "../../../../store/faq/faq.action";
import {CreateBreadcrumbs} from "../../../../store/breadcrumb/breadcrumb.action";
import {naviElements} from "../../../../app-navi-elements";
import {selectAvailableDataDomainsWithAllEntry} from "../../../../store/my-dashboards/my-dashboards.selector";
import {ALL_DATA_DOMAINS} from "../../../../store/app/app.constants";
import {Announcement} from "../../../../store/announcement/announcement.model";
import {MarkUnsavedChanges} from "../../../../store/unsaved-changes/unsaved-changes.actions";
import {BaseComponent} from "../../../../shared/components/base/base.component";

@Component({
  selector: 'app-faq-edit',
  templateUrl: './faq-edit.component.html',
  styleUrls: ['./faq-edit.component.scss']
})
export class FaqEditComponent extends BaseComponent implements OnInit, OnDestroy {
  editedFaq$: Observable<Faq>;
  faqForm!: FormGroup;
  availableDataDomains$: Observable<any>;
  formValueChangedSub!: Subscription;

  constructor(private store: Store<AppState>, private fb: FormBuilder) {
    super();
    this.availableDataDomains$ = this.store.select(selectAvailableDataDomainsWithAllEntry);
    this.editedFaq$ = this.store.select(selectEditedFaq).pipe(
      tap(faq => {
        this.faqForm = this.fb.group({
          title: [faq?.title, [Validators.required.bind(this), Validators.minLength(3)]],
          message: [faq?.message, [Validators.required.bind(this), Validators.minLength(3)]],
          dataDomain: [faq && faq.contextKey ? faq.contextKey : ALL_DATA_DOMAINS],
        });
        if (faq.id) {
          this.store.dispatch(new CreateBreadcrumbs([
            {
              label: naviElements.faqManagement.label,
              routerLink: naviElements.faqManagement.path,
            },
            {
              label: naviElements.faqEdit.label,
            }
          ]));
        } else {
          this.store.dispatch(new CreateBreadcrumbs([
            {
              label: naviElements.faqManagement.label,
              routerLink: naviElements.faqManagement.path,
            },
            {
              label: naviElements.faqCreate.label,
            }
          ]));
        }
        this.unsubFormValueChanges();
        this.formValueChangedSub = this.faqForm.valueChanges.subscribe(newValues => {
          this.onChange(faq);
        });
      })
    );
  }

  navigateToFaqList() {
    this.store.dispatch(new Navigate('faq-management'));
  }

  saveFaq(editedFaq: Faq) {
    const faqToBeSaved = {id: editedFaq.id} as any;
    const formFaq = this.faqForm.getRawValue() as any;
    faqToBeSaved.title = formFaq.title;
    faqToBeSaved.message = formFaq.message;
    if (formFaq.dataDomain !== ALL_DATA_DOMAINS) {
      faqToBeSaved.contextKey = formFaq.dataDomain;
    }
    this.store.dispatch(new SaveChangesToFaq(faqToBeSaved));
  }

  openDeletePopup(editedFaq: Faq) {
    this.store.dispatch(new ShowDeleteFaqPopup(editedFaq));
  }

  getDeletionAction() {
    return new DeleteEditedFaq();
  }

  ngOnDestroy(): void {
    this.unsubFormValueChanges();
  }

  private onChange(editedAnnouncement: Announcement) {
    const faqToBeSaved = {id: editedAnnouncement.id} as any;
    const formFaq = this.faqForm.getRawValue() as any;
    faqToBeSaved.title = formFaq.title;
    faqToBeSaved.message = formFaq.message;
    if (formFaq.dataDomain !== ALL_DATA_DOMAINS) {
      faqToBeSaved.contextKey = formFaq.dataDomain;
    }
    this.store.dispatch(new MarkUnsavedChanges(new SaveChangesToFaq(faqToBeSaved), faqToBeSaved.id === undefined));
  }

  private unsubFormValueChanges() {
    if (this.formValueChangedSub) {
      this.formValueChangedSub.unsubscribe();
    }
  }

  override ngOnInit(): void {
    super.ngOnInit();
  }
}
